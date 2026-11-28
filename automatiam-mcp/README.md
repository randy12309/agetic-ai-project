# AutomatIAM MCP Server

Exposes approved, read-only OneIAM capabilities to AutomatIAM workflows as typed MCP tools.

It is an integration layer, not an agent and not an authorization authority. LangGraph orchestrates
workflows, this server exposes typed capabilities, OPA decides whether a tool may run, and the
OneIAM services enforce their own business rules and remain the authoritative source of IAM data.

Architecture, security model, tool catalog, the dependency compatibility report and the list of
unresolved integrations are in [`automatiam-mcp.md`](automatiam-mcp.md).

## Stack

| Component | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Spring AI | 2.0.2 (`spring-ai-starter-mcp-server-webflux`) |
| MCP Java SDK | 2.1.0 (transitive) |
| Transport | Streamable HTTP, stateless, async |
| Endpoint | `POST /mcp` |

This repository does **not** use `iam-spring-boot-starter-*`. Those are built against Boot 3 and
the enforcer rules in `pom.xml` fail the build if one arrives transitively.

## Running locally

Requires JDK 25. Everything below runs without enterprise network access beyond Artifactory for
dependency resolution.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile serves synthetic IAM data from in-process adapters. No OneIAM endpoint, OPA
sidecar or identity provider is contacted.

`MockGuard` refuses to start if `automatiam.mcp.mocks.enabled` is true without `local` or `test`
active, so this configuration cannot be carried into a deployed environment by accident.

### Against a real OPA sidecar

```bash
opa run --server --addr 127.0.0.1:8181 deploy/opa/policy
./mvnw spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--automatiam.mcp.mocks.policy-enabled=false
```

`deploy/opa/policy/tools.rego` is a development sample. The authoritative bundle lives in
`automatiam-policies`. See [`deploy/opa/README.md`](deploy/opa/README.md).

## Tests

```bash
./mvnw verify
```

195 tests. `verify` also runs the JaCoCo gate (90% instruction coverage; currently 95%) and the
enforcer rules. No test needs network access, a database, a container runtime or a real issuer.

Run one group:

```bash
./mvnw test -Dtest=McpProtocolIntegrationTest initialize, discovery, invocation, schemas
./mvnw test -Dtest=McpSecurityIntegrationTest authn, scopes, deny-by-default
./mvnw test -Dtest=ToolInvocationIntegrationTest  # per-tool behaviour end to end
./mvnw test -Dtest=ToolExecutorTest the execution pipeline
```

## Exercising the tools by hand

Every call needs a bearer token whose `groups` claim names an admitted Okta group (see
`automatiam.mcp.auth.required-groups`; the `local` profile admits the DEV-role groups). With a
local issuer, export it once:

```bash
TOKEN=...   # access token from your local issuer
MCP=http://localhost:8080/mcp
```

Discover the catalogue:

```bash
curl -s $MCP -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | jq
```

Call a tool:

```bash
curl -s $MCP -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{
        "name":"search_entitlements",
        "arguments":{"searchText":"claims","size":5}}}' | jq
```

Synthetic domain IDs in the `local` profile: `AB12345` holds three entitlements, `CD67890` holds
one. A token whose `domainID` claim is `AB12345` may read `AB12345` and is refused `CD67890`
unless it also carries an admin group (`ONEIAM-FULLADMIN-*` or `ONEIAM-READONLYADMIN-*`).

People are identified by **domain ID** - the Elevance Health network login identifier. The claim
priority (`heightenedDomainID`, then `domainID`, then `sub`) mirrors `DomainIdResolver` in
`oneiam-shared-models`. Unlike that resolver, this server ignores `X-Impersonated-User-Id`; see
`automatiam-mcp.md`, "Security model".

Pass '-H 'CorrelationId: my-trace-1'' on any call and it is echoed on the response and appears
on the audit record.

## Configuration

| Variable | Required | Notes |
|---|---|---|
| `AUTOMATIAM_MCP_OIDC_ISSUER_URI` | yes | No default. Startup fails without it rather than validating tokens against nothing. |
| `AUTOMATIAM_MCP_OIDC_AUDIENCE` | yes | Expected audience on inbound tokens. |
| `AUTOMATIAM_MCP_ONEIAM_BASE_URL` | outside local/test | Unset locally; the synthetic adapters stand in. |
| `AUTOMATIAM_MCP_REQUIRED_GROUPS` | yes | Comma-separated Okta groups admitted to `/mcp`. Empty admits nobody. |
| `AUTOMATIAM_MCP_LOG_FORMAT` | no | `ecs` for the Splunk-shaped JSON deployed environments emit. |

Full property list with defaults: `src/main/resources/application.yml`.

## Layout

```
src/main/java/com/elevance/automatiam/mcp/
|-- tools/            tool implementations by domain, plus the shared execution pipeline
|-- clients/oneiam/   typed adapters for OneIAM capability APIs, and synthetic implementations
|-- clients/opa/      policy decision point
|-- security/         execution context derived from validated token claims
|-- audit/            structured audit events, correlation, metrics
|-- model/            typed tool contracts
|-- config/           properties, security and WebClient wiring
deploy/eks/           Deployment, Service, ConfigMaps, NetworkPolicy, PDB
deploy/opa/           sample policy and sidecar notes
```

## Contributing

Agent instructions: [`CLAUDE.md`](CLAUDE.md). Coding standards come from the `.agent-instructions`
submodule; project-specific rules are in `automatiam-mcp.md`.
