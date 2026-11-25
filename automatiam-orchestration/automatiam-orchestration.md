# automatiam-orchestration

This service executes LangGraph workflows for OneIAM. It sits in the middle of the three-hop chain shown below. It is read-only. Nothing it exposes changes anything in IAM.

```text
oneiam-shell → iamigo-chat → iamigo-api → automatiam-orchestration → LangGraph
(host app)     (npm package) (Java/Boot 3) (Python/FastAPI)          (in process)
                             └─ Valkey     ├─ OPA sidecar (authorize)
                                (conversations)    ├─ EHAP/Horizon (retrieve, generate)
                                           └─ PostgreSQL (checkpoints, run metadata)
```

## What this change did

The graph, nodes, registry, executor, run store, policy client, streaming contract and EHAP adapters already existed. `api/routes`, `api/schemas` and `api/middleware` were empty packages, there was no FastAPI app, and nothing constructed `NodeDependencies`. This change exposed the library over HTTP and connected its clients.

| File | Responsibility |
| --- | --- |
| `api/app.py` | Application factory, lifespan and exception handlers |
| `api/routes.py` | Three contract endpoints and two health probes |
| `api/schemas.py` | Request and response bodies mirroring iamigo-api’s DTOs |
| `api/middleware.py` | Correlation, caller resolution and contract-version check |
| `api/dependencies.py` | Container for each client, the pool and the compiled graph |
| `__main__.py` | Entry point for `python -m automatiam` |

## The contract is not ours to choose

The merged `iamigo-api` is the only consumer. Its client defines the contract.

| Concern | Authority in iamigo-api |
| --- | --- |
| Paths, methods and bodies | `ReactiveOrchestrationClient`, `orchestration/dto/*` |
| SSE frame shape | `OrchestrationEventMapper` |
| Header names | `IamigoHeaders` |
| Workflow key and versions | `OrchestrationProperties` defaults |

| Method | Path | Response |
| --- | --- | --- |
| POST | `/v1/workflows/{workflowKey}/runs` | `text/event-stream` |
| GET | `/v1/runs/{runId}` | `{runId, conversationId, status, startedAt, completedAt}` |
| POST | `/v1/runs/{runId}/cancel` | HTTP 202 with `{runId, status}` |
| GET | `/health/liveness`, `/health/readiness` | Health probes |

`IamigoHeaders` sets `CorrelationId`, `X-Request-ID`, `X-IAMigo-Employee-Id`, `X-Orchestration-Contract-Version` and `Authorization`.

### The SSE envelope was wrong and is now flat

Previously, `StreamEvent.to_sse()` wrapped each event.

```json
{"eventId":"...","runId":"...","sequence":3,"timestamp":"...","type":"run.progress","payload":{"label":"Searching the IAM knowledge base"}}
```

`OrchestrationEventMapper.toPayload` reads `label`, `text`, `title`, `url`, `sourceId`, `status`, `category` and `runId` directly from the data object. Its `text()` helper returns null for a missing top-level scalar string. Nested labels and message text therefore appeared empty in an otherwise well-formed stream. A missing required `runId` on `run.started` raises `OrchestrationProtocolException`.

The corrected frame merges in the run ID and puts the sequence in the SSE ID field.

```text
id: 3
event: run.progress
data: {"runId":"run-...","label":"Searching the IAM knowledge base"}
```

IAMigo stamps its own `eventId`, `sequence` and `timestamp` in `IamigoChatService.event(...)`. The browser envelope in `iamigo-chat/src/api/types.ts` is built one hop later. The regression test `tests/unit/test_api_contract.py::test_event_data_is_flat_so_the_consumer_can_read_it` asserts that the nested `payload` is absent.

### Two divergences from the mock server, on purpose

The Java fixture is `iamigo-api/scripts/mock-orchestration-server.py`.

1. Cancellation reports the resulting status. The mock reports `"cancelling"` because it has no run store. A finished run retains its final status. `CancelResponse` passes this informational field through.
2. The service never emits `checkpointId`, `node` or `chunkText`. The mock emits them to test IAMigo’s allow-list. `NODE_PROGRESS_LABELS` maps node names to reviewed labels. An unmapped node emits no progress event.

## Security model

The four responsibilities in `core/security.py` are distinct.

- **Human identity.** The service receives an employee identity assertion from a trusted workload. It never authenticates the employee itself.
- **Workload identity.** A trusted header identifies the calling service.
- **Execution context.** Server-derived identity travels in LangGraph runtime context, keeping it out of checkpoint state.
- **Authorization.** OPA decides before work begins on every run.

The route and `WorkflowExecutor.start_run` enforce this order: authenticate → check contract version → validate body → resolve workflow → check message limit → check registry consumer list → ask OPA → claim conversation → execute. A refusal before streaming returns an HTTP status and JSON body. Once the first event commits the HTTP status,  the executor reports failure through a terminal `run.failed` event. IAMigo synthesises one if the stream ends without it.

### The hop that is not wired

`iamigo-api` sends `Authorization: Bearer <workload credential>`. This service reads `X-Workload-Identity` and does not validate the bearer. Without a bridge, real deployments fail closed with HTTP 401. The mock server hid this gap by skipping authentication.

ADR-010 selects a mesh sidecar or gateway that verifies the peer certificate and sets the trusted header, as assumed by `WorkloadAuthenticator`. The alternative is JWT validation as in `automatiam-mcp`, with an agreed issuer, JWKS and client identity claim. `WorkloadAuthenticator.authenticate` logs the specific cause of the 401 when the authorization header is present and the workload header is missing. `TestWorkloadAuthenticationGap` records the behaviour. For local development, `AUTOMATIAM_DISABLE_AUTH=true` resolves the caller to `local-development`, which the registry permits. `Settings` rejects this flag outside the local and test profiles.

## Configuration

Environment variables use the `AUTOMATIAM_` prefix. `Settings` prevents a deployed profile from starting with a mock or authentication bypass enabled.

| Setting (without prefix) | Notes |
| --- | --- |
| `PROFILE` | `local`, `test`, `dev`, `uat`, `prod`. Gates the shortcuts below. |
| `KNOWLEDGE_SOURCE` | Enum with `horizon`, `knowledge-api`, `mock`. Cortex is discussed as a fourth source. |
| `EHAP_TOKEN_URL`, `EHAP_CLIENT_ID`, `EHAP_CLIENT_SECRET` | OAuth2 client credentials shared by generation and retrieval |
| `EHAP_CHAT_URL` | `POST /v2/text/chats` |
| `EHAP_SEARCHES_URL` | `POST /v2/document/searches`. Separate services share a gateway. |
| `POSTGRES_DSN` | When absent, uses an in-memory run store without checkpointing and logs a warning |
| `POSTGRES_RUN_SETUP` | Creates the schema at startup. Deployed environments use a migration step. |
| `OPA_BASE_URL` | Loopback sidecar |
| `USE_MOCK_EHAP`, `USE_MOCK_OPA`, `DISABLE_AUTH` | Restricted to local and test |
| `PORT` | Listen port, default 8080 |

### Running it locally

```bash
AUTOMATIAM_PROFILE=local AUTOMATIAM_PORT=8099 \
AUTOMATIAM_USE_MOCK_EHAP=true AUTOMATIAM_USE_MOCK_OPA=true \
AUTOMATIAM_KNOWLEDGE_SOURCE=mock AUTOMATIAM_JSON_LOGS=false \
python -m automatiam

curl -sN -X POST localhost:8099/v1/workflows/iam.knowledge-assistant/runs \
  -H 'Content-Type: application/json' -H 'Accept: text/event-stream' \
  -H 'X-Workload-Identity: iamigo-api' -H 'X-IAMigo-Employee-Id: AB12345' \
  -H 'CorrelationId: corr-local-1' \
  -d '{"workflowVersion":"1","conversationId":"conv-local-1",
       "input":{"message":"How do I request access to Ping?"},
       "context":{"requestId":"req-local-1","channel":"iamigo"},
       "options":{"stream":true}}'
```

### One connection pool, and why its row factory matters

`AsyncPostgresSaver` opens cursors with `row_factory=dict_row` and requires `autocommit=True, prepare_threshold=0` on the pool. The run store shares the pool and reads rows by column name. Its previous positional indexing could read the wrong field or raise an unrelated error. Mypy caught this when the pool was shared. A second pool would double the connections.

## Production integration requirements

1. **Workload authentication.** The bearer-to-workload identity gap blocks deployment.
2. **PostgreSQL DSN.** Without checkpointing, conversations cannot survive restart. Single execution per conversation is enforced only within a process, so two pods can accept overlapping runs.
3. **Checkpoint retention.** `RetentionPolicy.checkpoint_ttl_seconds` defaults to `None`. Set an explicit checkpoint TTL. `SessionLifecycleService.cleanup_expired` requires a value. Cleanup is unscheduled and nothing calls `terminate_session`. Expiring a Valkey conversation key leaves its PostgreSQL checkpoint intact.
4. **AutomatIAM client ID for EHAP.** The shared-services credential was borrowed for testing.
5. **OPA bundle delivery.** Deliver `automatiam/workflow` from S3 with IRSA, following `automatiam-mcp`.

## Not built, on purpose

- **Token-by-token model streaming.** `chunk_answer` splits an answer after grounding validation. Showing text before the full answer is validated would change the product’s grounding guarantee.
- **Conversation history in the start request.** History lives in IAMigo’s Valkey and the service’s checkpoints. The contract omits it and the route deliberately passes an empty list.
- **Durable async runs.** `ExecutionMode` has one member. Adding another requires a registry change.
- **CORS.** The browser calls IAMigo. It has no reason to call this service directly.

## Open questions

1. **Workload authentication mechanism.** Mesh-injected header or validated JWT. Blocking.
2. **Evidence approval.** `AUTOMATIAM_EHAP_APPROVE_BY_RANK=true` makes `HorizonSearchClient` approve the three highest-ranked chunks as `SUFFICIENT`. Rank does not establish that the chunks answer the question. This can produce the false-answerable outcome measured by the golden dataset in `automatiam-knowledge`.

   The heuristic defaults off. Enabling it logs a startup warning and stamps assessments with `reason_code=rank_threshold_heuristic` and `policy_version=rank-threshold-heuristic-0` for audit. Without an assessment, `approved_evidence_ids` is empty and `retrieve_knowledge` retains only `response.approved_evidence()`. A curl request returned eight chunks while the chat displayed no source chips and asked for clarification.

   The proposed fix removes `_rank_threshold_assessment`. The knowledge API implements `EvidenceDecisionPolicy`, `CoverageTrigger`, `RelevanceEvaluator` and `SourceValidator`. This service supports `KNOWLEDGE_SOURCE=knowledge-api` at `/v1/knowledge/search`. The remaining work is replacing `MockVectorRetriever` and `MockKeywordRetriever` with retrieval through `POST /v2/document/searches`, which the source reports working against the live corpus.

   Citations read the `sourceUrl` tag on each EHAP chunk. A document with that tag produces a linked source chip. A document without it still produces a chip.
3. **Checkpoint retention period.** See the production requirements.
4. **Cancellation semantics.** `cancel_run` records cancellation but does not interrupt a graph mid-node. A long retrieval can remain in flight after cancellation is reported.
5. **Channel.** The service accepts and logs `channel`. Nothing else reads it.

## Design note: curating and masking what reaches the gateway

Handoff proposal. The masking design is unimplemented and calls for security review.

### The gateway supports a system role — verified

The source records a live run of `scripts/probe_horizon_roles.py` on 2026-11-16. The gateway accepted `system` and honoured a system-only instruction. It accepted `assistant` and rejected `developer` with HTTP 422. `HorizonEhapClient` now sends instructions as a system message and the evidence and question as user content. An earlier docstring incorrectly inferred that the gateway had no system role.

The probe tried “ignore all previous instructions” in user content with both separate and folded instructions. The model resisted both cases. That probe did not establish the strength of role separation against instructions disguised as article prose. Grounding validation remains the control.

### Curate before masking

Each MCP tool should declare the fields from its typed records that may enter a prompt. Compose only those fields into evidence. Omitting a field eliminates its exposure, while masking reduces it.

### Masking is pseudonymisation, not obscurity

The tokenisation mechanism can be public. The surrogate mapping protects the data.

`generate_answer` builds blocks from evidence excerpts and passes them to `GenerationRequest.evidence_blocks`. This is the single path from evidence to the gateway.

- **Use per-run surrogates.** Sequential identifiers need only be stable within the request. A deterministic HMAC shared across runs would let the gateway correlate people across conversations.
- **Mask before evidence enters state.** Checkpoints persist excerpts. Store surrogates there and keep the mapping in runtime context for the life of the run, as with identity.
- **Reject unknown surrogates on output.** An unmapped surrogate is a hallucinated identifier. Fail the answer.

### Three ways it breaks

1. **Re-substitution is fragile.** A model can rewrite `[[P1]]` as “Person 1”. An opaque format reduces rewriting. A system instruction can reduce how often tokens change, but the round-trip check must catch any remaining changes. A deterministic round-trip validator enforces the requirement after generation.
2. **Masking can remove the signal.** A naming convention may itself be the evidence that an entitlement looks like a service account. Choose fields according to what the question needs.
3. **Quasi-identifiers can still identify the subject.** The source’s SQL job example ends in `syspolicy_purge_history` (the middle of the identifier is clipped). Even with the owner’s name masked, the entitlement string can reveal the system and team even when the person is masked. Identify those fields explicitly.

### The failure a table lookup cannot catch

A transposition swaps two valid surrogates and attaches an attribute to the wrong entity. Both tokens round-trip successfully. Where possible, send one surrogate for a question about one entity. When several are necessary, retain field-level provenance: tool, entity ID, timestamp and authorizing OPA decision.

### Which is also what knowledge-plus-data needs

“Who can approve this entitlement?” needs an entitlement-management record and a FAQ procedure. `validate_grounding` expects lexical overlap with evidence excerpts. A tool returning `{owner, approvers}` provides no prose to overlap and a correct answer can route to controlled failure.

- **Knowledge evidence** uses citations and overlap.
- **Data evidence** needs provenance and field-level attribution. Re-reading the underlying record verifies the claim directly and gives stronger evidence than lexical overlap.

Keep this as a second workflow, `iam.entitlement-assistant`, with its own evidence model, validation and failure modes. It can share MCP and OPA with the knowledge workflow.

## Design note: screen-aware answers and real IAM data

This handoff proposal is also unimplemented. Its motivating question is whether the assistant can definitively identify who can approve the entitlement on screen.

### Implementation order

1. **Workload authentication.** The rest depends on resolving the first deployment gap.
2. **MCP integration into the graph.** The source reports no MCP client, tool node or routing between knowledge and data questions. Its search found no `mcp`, `tool_call`, `ToolNode` or `bind_tools` integration. `NodeDependencies` exposes knowledge and EHAP clients.
3. **A real tool backend.** Of nine clients in `automatiam-mcp/clients/oneiam/`, only `HttpEntitlementDetailClient` had a real HTTP implementation. `get_manager_metrics` had an interface and `MockManagerMetricsClient` over `SyntheticIamData`. Composition, contract, authorization and tests existed, while HTTP calls were missing. `AUTOMATIAM_MCP_ONEIAM_BASE_URL` defaults empty.
4. **Screen awareness.** Build the visible feature after the data path can answer it.

### Screen awareness and the contract change

The host knows which entitlement is on screen. A `pageContext` provider prop would carry that context with the chat request. This affects `iamigo-chat` types, `iamigo-api` DTOs and its event allow-list, and this service’s schemas. The change spans four repositories and a package publish. `resolve_question` should resolve “this” against the current page rather than the previous turn. Keep resolution mechanical so a model cannot invent a missing entitlement.

### Page context is an untrusted hint

Browser-supplied page context selects the object of the query. It provides no permission to see it. Authorize the tool call independently against OPA for the given entitlement and user. In `automatiam-mcp`, a tool argument cannot construct a `SubjectContext`.

### Running the EM service locally

The proposal adds `oneiam-entitlement-manager-service` and its UI to local Compose against the dev (`slvr`) database. Keep authorization enabled. `EntitlementViewerResolver` resolves the caller’s Domain ID through `DomainIdResolver`, checks the view-all role and returns an `EntitlementViewer` carrying `applyOwnershipFilter`. `EntitlementAccessAspect` enforces caller groups per method. Disabling authentication can leave no viewer, or bypassing the aspect can expose every entitlement.

The approach in `iamigo-api/local` uses a local RSA public key for the issuer and a token with the real Domain ID. Identity resolution and ownership filtering then operate during the demo. Using dev data sends real entitlement information to EHAP for generation and persists evidence excerpts in local PostgreSQL checkpoints. The live-test docstring calls for “nothing resembling IAM data in a prompt sent to a shared gateway”. Review the masking design before using that data.

## Verification status

These results are reported in the source screenshots.

| Claim | Evidence recorded |
| --- | --- |
| Endpoints, bodies and headers match the consumer | `ReactiveOrchestrationClient`, its DTOs and `IamigoHeaders`, asserted in `test_api_contract.py` |
| Flat SSE matches the parser | `OrchestrationEventMapper.toPayload` and its `text()` helper |
| Workflow key, versions and channel defaults agree | `OrchestrationProperties` |
| Browser contract unaffected | `iamigo-chat/src/api/types.ts` compared with `ChatEvent` and `ChatEventPayload` |
| `AsyncPostgresSaver` requires `dict_row` | Installed package source |
| 81 tests passed, Ruff and mypy clean | `pytest -q`, `ruff check src tests`, `mypy src/automatiam` |
| Streaming, status, cancellation and ownership over HTTP | `python -m automatiam` with mocks, exercised with curl |
| Containerized chain with PostgreSQL and OPA | `local/docker-compose.yml` plus iamigo-api’s overlay reportedly ran six scenarios on 2026-11-16. Streaming worked, the edge overrode a forged workload header, bypassing the edge returned 401, and OPA allowed a run then failed closed without creating a run row. Eight checkpoints contained no identity. Ownership returned 404, and the partial unique index rejected a second running run. |

The source also retains an earlier **“Not verified”** note listing real PostgreSQL, an OPA sidecar and a browser end-to-end call. It says integration tests skip without a local database and the live EHAP test is deselected without a credential. The later containerized result above records different coverage. Neither establishes a live browser-to-EHAP test.

## POC demo scope and team hand-off — 2026-11-16

The source describes a proof of concept being demonstrated. Production hardening remains team work.

### Demo-ready

- The full path runs from `oneiam-shell` through `iamigo-chat`, `iamigo-api`, this service and LangGraph to EHAP/Horizon, with mocks available for OPA, generation and retrieval.
- The three contract endpoints cover SSE streaming, run status and cancellation with ownership enforcement.
- The source reports 81 tests, clean Ruff and mypy checks, and real HTTP requests with curl.

For the recorded demo, use `KNOWLEDGE_SOURCE=mock`. With `horizon` and no evidence assessment, the assistant asks for clarification on every question. See open question 2.

### Hand-off list, highest value first

1. **Workload authentication.** Bridge the bearer credential and trusted identity header using the ADR-010 mesh approach or JWT validation, as discussed above.
2. **Prompt injection hardening.** See `workflows/knowledge_assistant/prompts.py` and `integrations/ehap/horizon.py`.
   - Preserve architectural containment. This workflow has no tool that changes IAM. A generated answer must never become a tool call, parameter or authorization input.
   - The injection paragraph in `SYSTEM_INSTRUCTION` mitigates risk. Other model-readable text can defeat it.
   - `validate_grounding` checks citations and lexical support. A poisoned article can pass both because the answer faithfully quotes approved evidence. These checks address hallucination. They cannot establish the provenance of an article.
   - Add an output-side refusal for claims that access was granted, approved or changed. Enforce it after generation beside `_has_lexical_support`.
   - The `_INJECTION` fixture in `integrations/knowledge/mock.py` needs an assertion. The handoff identifies wiring this fixture into a test as the lowest-effort item.
   - The historical handoff describes `HorizonEhapClient._compose` folding instruction, evidence and question into one user message with the `--- EVIDENCE ---` delimiter. The body shape was checked against `oneiam-shared-services` (`aivalidation`). The gateway probe documented above subsequently confirmed support for the system role.
   - Control who can publish into the EHAP corpus. Coordinate corpus write governance with `automatiam-knowledge`.
3. **Database-backed prompts, partially built.** The aim is to change wording without a deployment.
   - `workflows/knowledge_assistant/prompts.py` defines `PromptKey`, the packaged baseline and three user-facing response strings moved out of `nodes.py`.
   - `integrations/persistence/prompt_store.py` contains the `PromptStore` protocol, `PackagedPromptStore`, `PostgresPromptStore`, the schema and `seed_statements` for the Liquibase seed.
   - `orchestration/prompt_catalog.py` contains a TTL cache, single-flight refresh, fallback to last known good then packaged prompts, checksum rejection and `versions()` for stamping a run.
   - Nodes still import `SYSTEM_INSTRUCTION` directly. `NodeDependencies` has no catalogue, the container builds none, `deploy/liquibase/` is absent and the source reports no tests for these modules. Finish the integration or remove it.
   - Git is the source of truth and the database distributes prompts. Each row carries a SHA-256 of its body. The service rejects mismatches, including a direct `UPDATE ... SET body`. A database-editable prompt is a higher-privileged injection vector than corpus text because the prompt supplies the instruction itself. Editing instructions can override protections that corpus content must contend with. Keep production prompt writes out of a direct-edit UI.
   - `CLARIFICATION_INSTRUCTION` was removed as dead code. `request_clarification` returns a fixed string without calling the model.
4. **Production integration.** Supply a PostgreSQL DSN, checkpoint retention, an AutomatIAM EHAP client ID and OPA bundle delivery. The DSN and retention particularly matter across pods.
5. **Citation URLs reach the browser unchecked.** `citation.url` passes from the corpus through `OrchestrationEventMapper` to `iamigo-chat`. A poisoned article can supply a malicious link without involving the model. Allow-list domains rendered as links. The source tracks the fix in `iamigo-chat.md`.

## Local end-to-end stack

The local stack runs on one machine without VPN, Okta or a deployment. It uses PostgreSQL checkpoints, OPA with the actual policy from `automatiam-policies`, and workload authentication. Only generation and retrieval are mocked.

The source refers to `local/README.md` for cold start, UI, three demo questions and expected answers, five security scenarios, environment and teardown. It identifies `local/scripts/demo.sh` as the script that runs them and prints expected observations.

The nginx `edge` container models the trusted mesh boundary. It uses `proxy_set_header` to replace inbound `X-Workload-Identity`, so clients cannot choose their workload identity. With `AUTOMATIAM_DISABLE_AUTH=true`, the caller would resolve to `local-development`, which the real policy denies. That shortcut would require mocking OPA as well.

## Repository layout visible in the editor

```text
automatiam-poc-source-2026-11-16/
├── .vscode/
├── automatiam-knowledge/
├── automatiam-mcp/
├── automatiam-orchestration/
│   ├── .agent-instructions/
│   ├── .github/
│   ├── .venv/
│   ├── deploy/
│   ├── local/
│   ├── scripts/
│   ├── src/
│   ├── tests/
│   ├── .gitignore
│   ├── .gitmodules
│   ├── automatiam-orchestration.md
│   ├── CLAUDE.md
│   ├── Containerfile
│   └── pyproject.toml
├── automatiam-policies/
├── docs/
├── iamigo-api/
├── iamigo-chat/
├── oneiam-shell/
├── .gitignore
└── WellPoint Internal Root CA 4.pem
```


