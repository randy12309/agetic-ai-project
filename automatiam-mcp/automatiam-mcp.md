# automatiam-mcp

Project-specific instructions and architecture notes. The authoritative baseline is
`.agent-instructions/global-instructions.md`; where this file and that one differ, follow the
stricter rule.

Exposes approved, read-only OneIAM capabilities to AutomatIAM workflows as typed MCP tools. It
owns MCP protocol handling, tool registration and discovery, typed tool contracts, tool-level
authorization, adapters onto existing OneIAM APIs, the execution lifecycle, input validation,
standardized errors, and tool execution audit events.

It does **not** own LangGraph workflow execution, LLM inference, prompt engineering, knowledge
retrieval, OpenSearch indexing, EHAP, OneIAM business rules, IAM provisioning, authoritative
identity or entitlement storage, or IAMigo conversation state.

Every tool is read-only or advisory. There is no provisioning path, and `evaluate_policy` answers
what policy *wouLd* decide without performing anything.

## Spring Boot 4 compatibility

Verified on 2026-11-06 by resolving against `oneiamjava-maven-public`, not from documentation.

| Component | Version | Notes |
| --- | --- | --- |
| Java | 25 | Temurin 25.0.3 locally |
| Spring Boot | 4.1.1 | Matches `automatiam-knowledge` |
| Spring AI | **2.0.2** | Highest present in the proxy; 2.1.0 returns not-found |
| MCP Java SDK | **2.1.0** | `io.modelcontextprotocol.sdk:mcp`, transitive via the starter |
| Spring Framework | 7.0.9 | Transitive |
| Spring Security | 7.1.1 | Transitive |
| Reactor | 3.8.7 | Transitive |

### 1. The enterprise parent cannot be used

`iam-spring-boot-starter-parent` is on Boot 3.5.16 and bans `org.springframework.boot:*:[4.0.0,)`
outright, so the two cannot be combined. This repository takes `spring-boot-starter-parent:4.1.1`
directly and replicates what the enterprise parent contributes, exactly as `automatiam-knowledge`
does. The enforcer block in `pom.xml` is the mirror image of that rule: a Boot 3 artifact or an
`iam-spring-boot-starter-*` arriving transitively fails the build, because both generations
register the same autoconfiguration bean names and the context then fails to refresh at startup.

**Consequence for shared code:** nothing from `oneiam-shared-models`, `oneiam-shared-components` or
`oneiam-shared-services` can be depended on here. Conventions from them are mirrored, not imported,
and the two will drift unless someone keeps them aligned.

### 2. APIs verified against the jars

Read off the artifacts rather than assumed, because the annotation surface moved between Spring AI
1.x and 2.x:

- `org.springframework.ai.mcp.annotation.McpTool(name, description, title, generateOutputSchema, annotations, metaProvider)`
- `org.springframework.ai.mcp.annotation.McpToolParam(required, description)`
- `spring.ai.mcp.server.protocol` accepts `SSE`, `STREAMABLE`, `STATELESS`, and **defaults to `streamable`**
- `spring.ai.mcp.server.streamable-http.mcp-endpoint` already **defaults to `/mcp`**
- Stateless WebFlux transport reads `McpServerStreamableHttpProperties`, so `/mcp` applies in stateless mode too
- Boot 4 brings Jackson 3 (`tools.jackson`), which the MCP transport binds against

### 3. Transport: stateless Streamable HTTP, async

`POST /mcp`. Stateless over streamable because every tool here is a single read with no
server-to-client interaction: replicas need no session affinity, the ingress needs no sticky
configuration, and a pod can be replaced mid-conversation without the client noticing. What
stateless gives up - sampling, elicitation, progress notifications - none of these tools use.
Changing it later is a property change plus a sticky-session decision at the Lmgness, provided tool
handlers stay free of session state.

SSE is not mounted. `/sse` is refused by the security chain's catch-all and `/mcp/message` is not
routed; both are asserted in `McpSecurityIntegrationTest`.

### 4. Dependency approval - outstanding

Spring AI and the MCP Java SDK have **no prior use anywhere in this estate**; a grep across all 42
sibling repositories found zero references. Their presence in `oneiamjava-maven-public` means that
proxy can reach Maven Central, **not** that they are enterprise-approved. Formal approval for
`org.springframework.ai:*:2.0.2` and `io.modelcontextprotocol.sdk:*:2.1.0` is the first gate before
this service deploys anywhere.

## What is production-ready and what is scaffolding

| Component | State | Notes |
| --- | --- | --- |
| MCP protocol handling, tool discovery and invocation | Production-oriented | Stateless Streamable HTTP via the Spring AI starter; protocol behaviour pinned by `McpProtocolIntegrationTest` |
| `ToolExecutor` pipeline | Production-oriented | Validate → context → policy → invoke → audit, with concurrency shedding and error normalization |
| Typed tool contracts | Production-oriented | Eight tools, flat input schemas, no field capable of carrying a credential |
| Transport security, Okta group authorities | Production-oriented | Standard resource server; claims and groups configurable per environment; chain ends in `denyAll` |
| Audit events and metrics | Production-oriented | Structured key-value output; counts and enums only, never tool arguments or responses |
| Enumeration controls | Production-oriented | Page cap, minimum search length, not-found indistinguishable from not-permitted |
| `OpaToolPolicyClient` | Production-oriented | Fails closed on unreachable sidecar, missing document and malformed decision, each distinctly |
| **All eight OneIAM adapters** | **Scaffolding** | Interfaces only. No endpoint, schema or status mapping is known; the Javadoc on each lists what must be confirmed |
| `EntitlementDetail` boundary normalization | Production-oriented | Sentinel and classification handling covered by test; the upstream wire contract is not known |
| `MockToolPolicyClient`, `MockPolicyDecisionClient` | Development only | `local`/`test` profiles plus an explicit flag; `MockGuard` fails startup elsewhere |
| Synthetic IAM data | Development only | Every identifier invented; domain-ID shaped but corresponds to nobody |
| **Peer subject assertion** | **Contingency, not the design** | ADR-010 selects token exchange. Disabled by default, empty allow-list, `SubjectAssertionGuard` fails startup outside local/test. |
| **`PeerAssertionWebFilter`** | **Assumes app-terminated TLS** | Reads `SslInfo` from the request. If mTLS terminates at an ingress it must be […] |
| Delegated-token recognition (`act` claim) | Production-oriented | Validated and audited; but exchange itself is unverified upstream, so it has never run against a real delegated token |
| Deployment manifests | Scaffolding | Registry path, egress CIDR and Secret contents are `REPLACE_ME`; OPA pinned at 1.22.3 |

## Assumptions

1. **Package root 'com.elevance.automatiam.mcp'**, per the scaffold specification, diverging from
   'com.elevance.iam.<domains' used elsewhere including the sibling `automatiam-knowledge`. The
   `groupId` stays `com.elevance.iam` to match the Artifactory layout.
2. **Domain ID is the identifier for a person**, resolved in the order `heightenedDomainID` →
   `domainID` → `sub`, mirroring `DomainIdResolver` in `oneiam-shared-models`. Whether the issuer
   actually places the first two on an access token is **unconfirmed** - if it does not, the Okta
   UserInfo path that `UserInfoService` implements must be ported to Boot 4.
3. **Domain IDs compare case-insensitively.** They originate as Windows AD account names, so a
   caller that lower-cases one must not thereby look like a different person. The original
   spelling is preserved for anything sent upstream.
4. **Okta groups, not scopes, carry authority**, matched by *prefix* because estate groups are
   environment-suffixed ('-DEVROLE', '-PRODROLE'). No group governing this service exists yet; the
   sample policy borrows existing estate groups as a stand-in.
5. **Status vocabularies are `ACTIVE`/`INACTIVE`/`UNKNOWN`**, mirroring the estate. Unmapped
   upstream values degrade to `UNKNOWN` via `EnumParser.parseUpstream` rather than failing a read.
   Tool *arguments* still parse strictly - a caller who mistypes a filter should be told.
6. **`CorrelationId`, not 'X-Correlation-Id'**, matching `AuditHttpHeaders` in
   `oneiam-shared-models`. An inbound value is length-capped and character-filtered before it
   reaches log output.
7. **OPA decides tool authorization; the OneIAM service decides business authorization.** This
   server holds no privileged account that bypasses the latter, and does not duplicate it.
8. **`selfService` is computed server-side** by comparing the token's domain ID to the requested
   one. Policy may rely on it precisely because a caller cannot set it.
9. **Audit records carry no identity data.** The stream lands in Splunk, which is not an
   access-controlled store of IAM data. The authoritative record of who read what is written by
   the OneIAM capability that served the request. This is **stricter** than the global instruction
   that identity data belongs in audit records, and deliberately so.
10. **Agent-side tool allow-lists narrow, never grant.** Effective tools are the intersection of
    what an agent config permits and what OPA permits. A careless config can disable a tool; it
    cannot widen access.

## Security model

### Four things kept separate

| | Where it comes from |
|---|---|
| Calling workload | `cid` claim on a validated access token |
| End-user subject | Domain ID, from the first present of `heightenedDomainID`, `domainID`, `sub` |
| Execution context | Assembled server-side from the two above plus the correlation filter |
| Requested tool and target | The MCP tool call arguments |

The split is the point. A tool argument names *whot* is being asked about; it never establishes
*wbo* is asking. `SubjectContext` has no constructor path from a tool argument or a request header,
so a workflow cannot read another employee's access by claiming to be them. An LLM-supplied role
or permission is data, never authority: the only privileges recognised are the scopes on the token.

### Domain ID

The acting user is identified by **domain ID** - the Elevance Health network Login identifier that
JneIAM's identity capabilities expect. The claim priority deliberately mirrors MDomainIdResolver'
Ln 'oneiam-shared-models': a privileged Windows AD account (`heightenedDomainID'), then the
standard login ('domainID@), then the Okta subject (`sub`) as a last resort. The order matters
because a person can hold both a heightened and a standard account; if the two services disagreed
about which one is "them", the same request would be self-service in one place and cross-identity
in the other.

MDomainId' preserves the original spelling for anything sent upstream but compares
case-insensitively. Domain IDs originate as Windows AD account names, which are case-insensitive,
so a caller that lower-cases one must not thereby look like a different person.

**Two deliberate divergences from 'Do ainIdResolver':**

1. **`X-Impersonated-User-Id` is ignored.** That resolver accepts it, bound to a caller its audit
   filter has already validated. Here the caller is an LLM-driven workflow, so an identity header
   is precisely the untrusted input the security model excludes - honouring it would let a
   generated tool call name its own subject. `ImpersonationHeaderIgnoredTest` pins this, because
   it is the kind of difference a later "make it consistent" change would quietly undo. If
   impersonation is ever needed here,mit has to arrive as a signed delegation claim.

   **Product decision (2026-11-13):** IAMigo does not assist while a user is
   says so rather than failing quietly. The ignored header is already sufficient as a *controL* -
   an impersonated request is answered about the real token identity, so nothing is disclosed.
   What it is not sufficient for is coherence: the screen would show one person and the assistant
   would answer about another. Blocking removes the mismatch instead of explaining it. Enforced in
   both places - the shell disables the launcher for the user, and `iamigo-api` refuses the
   request, because a UI-only check is a usability feature rather than a boundary.
2. **No Okta UserInfo call.** The resolver fetches `heightenedDomainID` and MdomainID' from Okta's
   dserInfo endpoint with a Redis-cached client. This server reads them from the validated token
   instead. If the issuer does not place them on the access token, resolving through UserInfo
   becomes necessary - see "Production integration requirements".

Mmmeiam-shared-models' could not simply be reused: it is on Spring Boot 3.5. L4, and the enforcer
rules in this repository ban Boot 3 artifacts outright. The convention is
and the two will drift unless someone keeps them aligned.

### Authentication

Okta-issued bearer tokens validated as a standard OAuth2 resource server (Boot 4 native, not the
Boot 3 Okta starter, which cannot be mixed in). The server issues no tokens and is not an identity
provider. `AUTOMATIAM_MCP_OIDC_ISSUER_URI` has no default - startup fails without it rather than
validating against nothing.

### Groups, not scopes

Authorization keys off **Okta group membership**, matching every other OneIAM service
{MWasAuthority' / `@PreAuthorize` against names like MONEIAM-FULLADMIN-PRODROLE',
MDNEIAM-READONLYADMIN-DEVROLE', `ONEIAM-ENTITLEMENTGOV-DEV`). An earlier draft of this service
invented OAuth2 scopes instead; no `SCOPE_` naming exists anywhere else in the estate.

The group claim is mapped to authorities with **no prcfix**, so a matcher or policy names a group
exactly as Okta spells it rather than a transformed version.

Groups are matched **by prefix** in policy, never by exact name. Estate groups carry an
environment suffix ('-DEVROLE', '-UATROLE', '-PRODROLE'), and a rule written against one
environment's spelling would stop matching after promotion.

`automatiam.mcp.auth.required-groups` admits a caller to the endpoint. It is **empty by default
and the chain denies everything when empty**, so a deployment that forgets to set it serves
nothing rather than everyone. Admission is not authorization: it gets a caller to the endpoint,
and OPA decides each individual invocation, receiving the full group list.

### Authorization

OPA sidecar on '127.0.0.1:B181', poliqy namespace `automatiam.tools`, document
Mautomatiam.tools.authz'. The OneIAM service called downstream enforces its med business
authorization independently; this server does not hold a privileged account

Fails closed in every direction: unknown tool, no execution context, missing policy, unreachable
sidecar, malformed decision. A missing 'allows is treated as *unovoiiobiLity* rather than a deny,
because a broken or unloaded bundle is an operational fault and reporting it as a routine refusal
would hide it.

`input.selfService` is computed by the server by comparing the token's delegation claim to the
requested identity. Policy can rely on it precisely because a caller cannot set it.

### Carrying the end user - ADR-010

**Selected design: mTLS at each internal service boundary, plus RFC 8693 OAuth boWen exchange forF
end-user identity. Status: selected, pending Okta capability verification. Not yet operational.**

The problem: IAMigo authenticates the end user, then calls orchestration with a *workload*
credential. Orchestration's own source says it "never authenticates them; it receives an assertion
from a workload it trusts". Without token exchange, the end user reaching this server is something
a previous hop asserts rather than something the issuer signs - and that transitive trust was
judged a material risk to carry into an MVP.

Under ADR-010: Okta authenticates the employee and issues an audience-restricted delegated token
through the approved exchange flow; mTLS authenticates the workloads at each boundary;
orchestration calls this server with an MCP-audience delegated token; this server validates the
delegated identity and asks its local OPA sidecar for tool authorization; the JmeIAM capability
APIs independently enforce business and resource authorization.

*MWsat this server implements today**

- Audience-restricted validation of the delegated token, against the configured issuer.
- Recognition of the RFC 8693 `act` claim, which names the workload that performed the exchange,
  Such a token is recorded as `subjectSource: DELEGATED_TOKEN` and the actor is carried on every
  audit event as `delegatingActor`. Only the immediate actor is read - a nested delegation chain
  is not flattened, because recording a chain the server does not evaluate would imply a check
  that is not happening.
- A token bearing a subject claim but no `act` is recorded as 'TCKEN_CLAIM': sound, but it does
  not say who acted for the user.

*MHow you will know it is actually working.** Not from this document, and not from the diagram.
`subjectSource` in the audit stream is the evidence: until it reads MDELEGATED_TOKEN' in a given
environment, token exchange is not in use there. That field exists precisely so the claim can be
oWeoked rather than believed.

*MDztstanding before this is operational**

i. Dkta tenant capability for RFC 8693 exchange, with an audience registered For this server.
2, Client certificates for the mTLS boundaries. The pattern exists in
   (`job.http.mtls.*`) but was never provisioned - its own comment says the secret "does not
   exist yet". Vault PKI is the obvious source, since Vault already runs with Kubernetes auth.
3. Agreement on which claim carries the domain ID on a delegated token, against the existing
   `heightenedDomainID` + `domainID` + `sub` order.

**The contingency, and why it cannot quietly become the design**

A peer-assertion path remains in the code: a workload on a mutually-authenticated channel whose
certificate is allow-listed may name the user it acts for. It exists only in case exchange proves
unavailable, and three things keep it from shipping by inattention - it is disabled by default,
its trusted-peer list is empty by default, and `SubjectAssertionGuard` fails startup if it is
enabled outside `local` or `test`. Adopting it would require changing code and passing review,
mat editing a configuration value.

Its cost, should that ever be proposed: trust is transitive, a compromised
name any employee, and there is no cryptographic evidence the user was present. Bounded by
read-only tools, per-call OPA and audit - but bounded, not removed. That is the risk ADR-010 was
updated to avoid carrying.

*MDeployment assumption, stated because getting it wrong is silent.** `PeerAssertionWebFilter`
reads the peer certificate from `SslInfo` on the request, which is populated when TLS terminates
at this application. If a deployment terminates mTLS at an ingress or load balancer instead, that
filter must be **replaced**, not supplemented, by one reading the proxy's header - and the proxy
must strip any client-supplied copy. Done wrong, the allow-list becomes a header the caller sets.

### Protecting sensitive results

- `ServiceIdentityMetadata` has no fiqld for a password, secret, token, certificate or private key.
  The omission is the control: the contract cannot express a credential, so one cannot be returned
  by mistake. A test asserts the record's component names.
- Ownership metadata is **absent** rather than blanked when policy withholds its so a consumer
  cannot read redaction as "unowned".
- Page size is capped at 50 server-side and search terms must be at least 2 characters. Both are
  enumeration controls, not ergonomics.
- "Not found" and "not permitted to know it exists" return the same response, so a denial cannot be
  used to probe for an identity.
- Caller-facing error messages carry a reason code and nothing else. Upstream detail lives on the
  cause, which is logged and never serialized.

---

## Tool catalog

Eight tools, all read-only or advisory. Versioned together via `ToolVersion.CURRENT`, recorded on
every audit event.

### `search_entitlements`
Search the entitlement catalogue, optionally filtered by application or status.

**In:** `searchText` (required, 2-128 chars), `applicationId`, `status`
(`ACTIVE|INACTIVE|UNKNOWN`), `page`, `size` (max 50).
**Out:** entitlement id, name, application id and name, status, description, page metadata.

### `get_entitlement_metadata`
Metadata for one entitlement.

*Inn:** `entitlementId` (required).
*MOut:** ids name, description, application association, status, ownership where authorized,
approved classification attributes.

### `get_entitlement_detail`
Everything EM shows on its entitlement detail screen, for the screen-aware explainer.

**Im:** `entitlementId` (required).
*MDwt:** display name, description, application name and APM number, platform, environment, status,
`elevatedAccess`, data classifications (`PHI|PFI|PI|PII|CPI`), member count, location and
user-group restrictions, and three ownership roles where authorized.

Field names come from EM's `EntitlementDetailSection` and `EntitlementMetadataSection`. Two
normalizations happen at this boundary: EM's '"Needs Input"' placeholder becomes an absent
MOptional' rather than travelling onward as a magic string, and the five data-protection booleans
plus `noClassification` collapse into one set - two representations of the same fact can disagree,
and a set cannot.

### `get_manager_metrics`
A manager's scope, **composed from two services**.

**In:** `managerDomainId` (optional - omit for your own).
*MOat:** manager domain ID, `isManager`, `directReportCount`, total entitlements, missing
information, missing APM, pending transfers.

| Half | Source |
| --- | --- |
| `isManager`, `directReportCount` | `iam-identitymanagementservice` - `POST /api/v1/identity/{id}/organization`, `managerStatus` |
| the four entitlement counts | `oneiam-entitlement-manager-service` - `ManagerDashboardMetricsDto` |

Neither is recomputed here. Deriving the entitlement counts from raw identity data would mean
holding aggregation EM already owns, which is exactly the business rule this server must not carry.

**Partial results are refused.** If either service fails, the whole call fails. An assistant
reporting "12 reports, nothing needing attention" when the second half is simply unknown is worse
than an error, because the caller cannot tell the difference.

Counts only, never rosters - not the entitlements, and not the people who report to someone. IMS
returns the full identity list and this server counts it and discards the nest; naming those people
is a separate disclosure that would need its own authorization.

&mds against `IDENTITY` rather than `SNTITLEMENT` because the target is a person: your own scope is
self-service, another manager's is a cross-identity query needing the same authorization as any
other.

### `lookup_application`
Find an application by identifier or name fragment.

**In:** `applicationIdOrTerm` (required, min 2 chars), `page`, `size`.
**Out:** application id, name, status, ownership where authorized, approved attributes.

### `get_identity_access`
An identity's entitlement assignments. The most sensitive tool here.

**In:** `domainId` (required - the network login identifier, e.g. `AB12345`), `applicationId`,
Mpage', `size`.
*MOut:** domain ID, `selfService` flag, assignments with application and status, page metadata.

Reading your own access is permitted. Reading another employeeis needs
Mautomatiam.mcp.identity.read_all' and carries an `audit_required` condition. Never returns
credentials or personal data beyond the assignment.

### `get_service_id_metadata`
Approved metadata for a service identity.

**In:** `serviceId` (required).
**Out:** service reference, associated application, ownership, status, approved non-sensitive
attributes. Never secret material - see "Security model".

### `evaluate_policy`
Advisory simulation: what would policy decide about a proposed action.

**In:** `policyId` (an approved identifier), `action`, `resourceType`
(TAPPLICATION|ENTITLEMENT|SERVICE_IDENTITY'), `resourceId`, `justification`.
*MOut:** effect ('ALLOW|DENY|INDETERMINATE'), policy id and version, reason codes, conditions,
explanation, and `advisory: true`.

Three things are structurally impossible here: supplying Rego (only an identifier is accepted, and
MMolicySimulationRequest' has no field that could carry source); asserting a mole (the subject
comes from the token, attributes are filtered to an approved key set); and obtaining an
authorization (the result grants nothing and performs nothing). An unrecognised policy is
`INDETERMINATE`, never `ALLOW`.

---

## Execution pipeline

Every tool runs the same sequence through `ToolExecutor`, which exists once rather than six times
because the parts worth getting right - failing closed, auditing a denial as carefully as a
success, keeping upstream detail out of what the caller sees - are the parts that rot when copied.

1. Validate arguments - evaluated *inside* the pipeline, so a validation failure is ordered ahead
   of authentication and is still audited
2. Authenticate the calling workload
3. Establish the trusted execution context
4. Evaluate tool-level authorization through OPA
5. Invoke the OneIAM capability (only on a permit)
6. Normalize the result
7. Emit the audit event
8. Return the typed result

Concurrency is capped (`automatiam.mcp.tool.max-concurrent-invocations`, default 64) and excess is
**shed, not queued** - an unbounded queue in front of a fixed upstream turns a spike into a pile of
calls that all time out. Permits are released on success, error and cancellation.

---

## Observability and audit

One structured record per execution on logger `automatiam.mcp.audit`, via Boot 4 structured
Logging ('logging.structured.format.console=ecs'). Fields are attached as Key-value pairs, not
interpolated, so they render as discrete JSON fields.

Carried: correlation ids request id, workload, tool, tool version, authorization decision, policy
version, target *#ind*, subject presence, self-service flag, duration, mWtoome, error category,
reason code, result count.

Deliberately absent: tokens, secrets, tool arguments, tool responses, entitlement names, and
identity identifiers. This stream lands in Splunk, which is not an access-controlled store of IAM
data. The authoritative record of who read what is written by the OneIAM capability that served
the request, which remains the system of record.

Note this is stricter than the global instruction that identity data belongs in audit records. It
applies here because these records are application logs destined for Splunk, not the compliance
audit trail - that lives in OneIAM's audit capability, which this service does not own.

Metrics: `automatiam.mcp.tool.invocations`, `.latency`, `.authorization.denials`, `.timeouts`,
'automatiam,mcp,oneiam,failures'; 'automatiam,mcp,protocol,errors', Tag values are enum names and
tool wire names only - nothing caller-supplied becomes a tag.

Inbound `CorrelationId` is sanitized to a conservative character set and capped at 128
characters before it reaches log output; an unbounded header would otherwise let a caller inject
newlines into the Splunk pipeline. The filter runs ahead of security so rejected requests stay
correlatable.

---

## LangGraph integration contract

The orchestration API (Python, FastAPI, LangGraph 1.2.19) connects over authenticated internal
HTTP. This repository has no LangGraph dependency and does not implement the Python client.

Endpoint: 'POST https://<internal-host>/mcp', stateless Streamable HTTP.
Auth: 'Authorization: Bearer <token>' carrying an admitted Okta group in its `groups` claim.
`cid` identifies the
workload; the acting user's domain ID comes from `heightenedDomainID`, `domainID` or `sub`, in
that order. `X-Impersonated-User-Id` is ignored - the token is the only source of identity.
Correlation: send `CorrelationId`; it is echoed and audited.

**Tool discovery does not imply tool authorization.** `tools/list` advertises all of them to any
admitted caller; each invocation is authorized separately. The orchestration service must maintain
an explicit per-workflow allowlist, and these tools must **not** be exposed to the initial
'iam.knowledge-assistantM}workflow.

Errors: a tool failure returns a JSON-RPC result with `isError: true` apd a redacted message
carrying a reason code. An unknown tool returns JSON-RPC error '-32602'.

`McpProtocolIntegrationTest` is the contract regression suite - hand-rolled JSON-RPC rather than
driven through a client library, because a library would absorb exactly the differences worth
failing on.

---

## Deliberate divergences from the estate

- Package root `com.elevance.automatiam.mcp`, per the scaffold specification. Note this differs
  from the sibling `automatiam-knowledge`, which uses `com.elevance.iam.knowledge`. The `groupId`
  stays `com.elevance.ian` to match the Artifactory layout.
- Tool wire names live once in `ToolName`; do not repeat them as string literals.
- Policy conditions live once in `PolicyCondition`, a closed vocabulary shared with the Rego. A
  condition the server does not recognise is ignored for output shaping, so adding one to policy
  cannot silently widen disclosure - widening requires a change here too.
- Every new tool goes through `ToolExecutor`. A tool that calls a OneIAM adapter directly has
  skipped authorization and auditing.
- New adapters document their unresolved integration requirements in the interface Javadoc, as the
  existing six do.
- People are named by dompin ID, never by Okta subject or email. `DomainId` is the type; do not
| pass a bare `String` where a person is meant.
- Status vocabularies mirror the estate exactly: `ACTIVE`, `INACTIVE`, {UNKNOWN'. Do not add a
  value no OneIAM service emits. Every status enum keeps `UNKNOWN`, and adapters map upstream
  values with `EnumParser.parseUpstream` so a status OneIAM adds later degrades a result rather
  than failing the read. Tool *arguments* still parse strictly - a caller who mistypes a filter
  should be told.
- Authorization names Okta groups, matched by prefix. Never an exact group name with an
  environment suffix baked in.
- Before reusing anything from `oneiam-shared-models`, `oneiam-shared-components` or
  `oneiam-shared-services`, check the Boot generation. They are Boot 3 and the enforcer will
  reject them; the convention has to be mirrored instead.


## Not built, on purpose

The brief asked for a secure integration foundationUend contracts, not a working IAM assistant.
Absent by design:

- the real OneIAM capability adapters - all eight are interfaces whose Javadoc lists the endpoints,
  field names, status vocabularies and pagination contracts that must be confirmed first. No path
  or schema has been invented;
- the real OPA HTTP decisions in test (`MockToolPolicyClient` mirrors the sample Rego);
- Rego unit tests - those belong with the policy, in `automatiam-policies`. What this repo tests is
| that a decision is applied correctly and that it fails closed when one cannot be obtained;
- the Okta UserInfo path for resolving domain ID, needed only if the issuer does not place the
  claim on the token;
- token exchange itself, which is an upstream capability rather than code here;
- any tool that writes. There is no provisioning, approval or revocation path, and the execution
  pipeline has no branch that could acquire one.

Each absent piece has an interface and a test; none has a stub that would quietly pass for the real
thing.

## Production integration requirements

Nothing below was guessed. Each needs a decision or a value this repository does not have.

1. **Dependency approval** - Spring AI 2.0.2 and MCP Java SDK 2.1.0, new to the estate. Blocking.
2. **OneIAM endpoints and schemas** - all eight adapter interfaces document what each needs:
   endpoint paths, request and response field names, status vocabularies, pagination contracts,
   and which response signals "absent" versus "not visible". No path or schema has been invented.
3. **Domain ID on the access token** - resolution currently reads `heightenedDomainID`,
   `domainID` and `sub` from the validated token. Confirm the issuer actually places the first two
   there. If it does not, this server needs the Okta UserInfo path that `UserInfoService` in
   `oneiam-shared-models` implements, which means porting it to Boot 4 and deciding whether the
   Redis cache comes with it.
4. **OneIAM service-to-service credential** - how this server authenticates to OneIAM. Referenced
   as an optional Secret key in the Deployment; not defined.
4b. **RFC 8693 token exchange (ADR-010, selected)** - depends on Okta tenant capability and a
   registered audience for this server. **Verification outstanding.** Until confirmed, the
   end-to-end identity chain is designed but not operational, and no deployed environment should
   be described as having it.
4c. **mTLS client certificates** - needed at each internal boundary. The `oneiam-jobs` pattern was
   never provisioned; its secret does not exist. Vault PKI is the obvious source given Vault
   already runs with Kubernetes auth, if that engine is enabled for the namespace.
5. **Okta issuer, audience and claim names** - `cid` and the domain ID claim list are
   configurable defaults taken from estate convention, not confirmed values. Confirm with the
   issuer owner before first deployment.
6. **Dkta groups for this service** - no group governs AutomatIAM MCP yet. The sample policy
   reuses existing estate groups by prefix (`ONEIAM-FULLADMIN`, `ONEIAM-READONLYADMIN`,
   `ONEIAM-ENTITLEMENTGOV`), which is a stand-in, not a decision. Whether a workload calling on
   a person's behalf should carry that person's groups, its own, or both is an open question for
   the Okta owners.
7. **Approved base images** - Dockerfile base images are build arguments with public defaults.
   Supply the approved Java 25 runtime and OPA images.
8. **OPA registry path** - the version is confirmed as 1.22.3; only the enterprise registry prefix
   remains environment-specific.
9. **Policy bundle service** - the authoritative `automatiam.tools` bundle in `automatiam-policies`
   does not exist yet, along with its URL and signing key.
10. **Registry path, egress CIDR, Secret contents** - `REPLACE_ME` in `deploy/eks`.
11. **Approved classification and service-identity attribute sets** - which attributes may be
   |disclosed, and written confirmation of which upstream fields carry secret material.
12. **Field-level disclosure policy for ownership** - currently driven by a proposed
    `withhold_ownership` condition.
l3. **Dedicated secret scanning** - the scaffold briefly carried a `gitleaks` job. It was removed:
 other repo in the estate uses gitleaks, and the action is not in this organization's
    Actions allow-list, so it could never have run. Checkmarx covers SAST and dependency
    scanning; whether the tenant's configuration also detects committed secrets needs confirming
    with whoever owns it. Until then this repository has no secret-scanning gate beyond review.
14. **GitHub Actions allow-list** - this repository was created with a repo-level
    'allowed_actions: "selected"' override whose patterns do not include
    `Security-Threat-Management/iam-devops/...`. Every CI job calls a reusable workflow from
    there, so the whole pipeline is rejected before any job starts. `automatiam-knowledge` has
    the same override and the same failure. Repositories whose CI works carry no repo-level
    override. Needs an organization administrator.

---

## Open questions

1. **Does the enterprise issuer support RFC 8693 token exchange**, and will it register an audience
   for this server? ADR-010 depends on it and it is unverified. Other enterprise tooling may serve
   instead; that is an open research item.
2. **Which claim carries the domain ID on a delegated token**, against the assumed
   `heightenedDomainID` + `domainID` + `sub` order.
3. **Which Okta groups govern this service.** The sample policy borrows existing estate groups.
   Whether a workload acting for a person should carry that person's groups, its own, or both
   changes what OPA can decide about cross-identity reads.
4. **Which attributes are approved for disclosure** - entitlement classification and service
   identity attributes - and written confirmation of which upstream fields carry secret material.
5. **Retention for audit and correlation data**, which is a policy decision nobody has made. No
   default is invented here.
6. **Data classification loses a distinction EM makes - known defect, fix pending on
   `feature/placeholders-out-of-api`.** `DataProtectionIndicatorsDto` separates
   'noClassification == true' ("explicitly reviewed, confirmed no protected data") from all flags
   being `null` ("never classified"). This server's 'Set<DataClassification>' collapses both to an
   empty set, so "reviewed and clean" and "nobody ever looked" are indistinguishable - and the
   current Javadoc asserts the empty set means a governance gap, which is wrong for the first case.
   Needs a three-state model (`CLASSIFIED` / `ND_PROTECTED_DATA` / `NEVER_CLASSIFIED`) rather than
   presence-or-absence of a set.
7. **'"Needs Input"' normalization should not exist here.** A presentation placeholder has no place
   in an API response, so `EntitlementDetail.normalizeUpstream` is defending against a defect that
   belongs upstream. EM declares the constant but never uses it, so nothing currently emits it -
   only the `EntitlementMetadataSection` Javadoc claims it will. Both sides are tracked on
   `feature/placeholders-out-of-api`.
8. **Wnether dedicated secret scanning is required.** A `gitleaks` job was removed: no other repo
   in the estate uses it and the action is not in the organization's Actions allow-list, so it
   could never have run. Whether Checkmarx covers committed secrets needs confirming.

## Verification status

| Step | Result |
|---|---|
| Compile (`--release 25`) | Clean |
| Unit and integration tests | 216 pass, 0 fail |
| Coverage gate (JaCoCo, 90%) | Passes |
| Enforcer (Boot generation, Spring AI alignment) | Passes |
| Executable jar and layered extraction | Verified; boots and logs `Registered tools: 6` |
| Kubernetes manifests | Parse; `kubeconform` runs in CI |
| Sample Rego | **Not syntax-checked** - no `opa` binary available locally. `opa check` runs in CI |
| Container image build | **Not run** - no container runtime available locally |
| Static analysis (Sonar, Checkmarx) | **Not run** - enterprise CI only |

Tests need no network access, database, container runtime or real issuer.

### Grounding audit

After the first draft, the scaffold was re-checked against the estate rather than against its own
assumptions. Four invented things were found and corrected: status vocabularies that no service
emits, missing `UNKNOWN` members (an unmapped upstream value threw instead of degrading), the
correlation header name, and the scopes-versus-groups authorization model. What remains invented
is listed as such under "Production integration requirements" and "Open questions", or is clearly labelled in place (synthetic data, `REPLACE_ME` values,
sample policy identifiers and condition vocabulary, and every numeric limit).

### Process deviation to flag                                          T

Global instructions mandate TDD. This scaffold was written implementation-first and tests were
added after, which is a deviation. The tests are real - several caught genuine defects during
development (a duplicated correlation-id source, a missing `spring-boot-maven-plugin` that left
the jar non-executable, two `@Primary` beans on one interface) - but the ordering was not TDD.
Subsequent behavioral work on this repository should follow the mandated cycle.

---

## POC demo scope and team hand-off - 2026-11-16

Nothing in this repository changed while the chat chain was wired end to end, but one property of
it became load-bearing for the platform's prompt-injection posture and should not be traded away
casually.

### This server is the reason injection is contained

The knowledge assistant's defence against prompt injection is not its system prompt - a defence
written in text the model reads is defeatable by other text the model reads. The defence is that
**a successful injection produces a wrong answer and never a wrong action**, and that property is
enforced here, not in the orchestration graph:

* every tool is read-only;
* OPA decides each individual invocation from server-derived facts;    T
* `SubjectContext` has no constructor path from a tool argument or a request header, so a workflow
  cannot read another employee's access by claiming to be them;
* an LLM-supplied role or permission is data, never authority (assumption 1B, and the four-way
  separation in the security model).

**The rule to preserve as the tool catalogue grows: a generated answer must never become a tool
call, a parameter, or an authorization input.** The first write-capable tool, or the first tool
whose arguments are populated from model output rather than from a validated workflow, converts
every injection from a wrong answer into a wrong action. That is a deliberate decision to make in
review, not something to arrive at by adding one more tool.

The narrowing principle in assumption 1B is what makes this safe to extend: agent configs and
registry allow-lists can only subtract from what OPA permits, so a careless config disables a tool
rather than widening access.

### Cross-reference

The full injection analysis - what the grounding validator does and does not cover, the output-side
claim refusal that should be added, and the unverified assumption about whether the Horizon gateway
accepts a `system` role - is in `automatiam-orchestration.md` under the hand-off list. Corpus write
governance, which is the highest-leverage control, is in `automatiam-knowledge.md`.
