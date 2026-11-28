# Local integration stack

Infrastructure for exercising `automatiam-mcp` against real dependencies instead of its in-process
mocks. Follows the estate convention in `oneid-serviceidservice/compose.yaml`: **compose provides
the infrastructure, the Java services run from Maven** with the `local` profile.

```bash
cd local
podman compose up -d
podman compose logs opensearch-seed     # should end with "seed complete"
podman compose down -v                  # tear down, including the volume
```

Tested with Podman 5.7.1. `podman compose` delegates to the `docker-compose` provider on this
machine; `podman-compose` is not required.

| Service | Port | Purpose |
| --- | --- | --- |
| `opensearch` | 9200 | Backs EM's entitlement-detail endpoint |
| `opensearch-seed` | - | One-shot. Creates and populates the three indices EM reads |
| `opa` | 8181 | Policy sidecar, serving `deploy/opa/policy` under the pinned 1.22.3 |

## What this verifies

`OpaSidecarIntegrationTest` runs against the live sidecar and **skips itself** when nothing is
listening, so `./mvnw verify` still works with no containers - the estate requires tests to run
without network access. It proves three things the stubbed tests cannot: that the request this
server sends is one real OPA accepts, that the sample Rego parses under the pinned version, and
that `MockToolPolicyClient` has not drifted from the policy it mirrors.

That last one has already earned its place twice. The sample policy **failed to parse** under
1.22.3 until a `default` rule stopped referencing the `policy_version` variable - a default rule's
value must be constant. Nothing in the Java test suite could have caught it.

## Running the MCP server against the real sidecar

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--automatiam.mcp.mocks.policy-enabled=false
```

That sends authorization and simulation requests to the sidecar while keeping the synthetic
OneIAM adapters active. With all mocks disabled, the catalog includes policy simulation and,
when a OneIAM base URL is configured, entitlement detail. The other tools require adapters
that currently exist only in the mock profile.

## Running against the real Entitlement Manager - read this first

`HttpEntitlementDetailClient` targets the contract discovered from
`oneiam-entitlement-manager-service`: `GET /api/v1/entitlement-detail?entitlementId=…` under base
path `/oneid-entitlement`. The mapping is covered by `HttpEntitlementDetailClientTest` against
EM's real response shape, so the adapter is believed correct. End-to-end against a running EM is
**not yet demonstrated**, for two reasons that are not in this repository's gift:

**1. EM still requires a validated token locally.** `LocalSecurityConfig` disables CSRF, basic and
form login but keeps `.anyExchange().authenticated()` with an OAuth2 resource server, and
`application-local.properties` points the issuer at `portalssoqa.elevancehealth.com`. So calling EM
from MCP needs either a real QA token or an issuer override to a local JWKS. Until one of those is
chosen, the hop cannot be exercised.

**2. Entitlement ids are OpenSearch document hashes, not `ENT-xxxx`.** EM's own Javadoc gives
`0abcfdb58d231b42818d28910e5d78b3` as the example, and the seed data uses that id deliberately.
The synthetic ids in `SyntheticIamData` are a different shape on purpose - they belong to the mock
world. Anything demonstrating the real path must use a hash.

To try it once those are resolved:

```bash
# EM, from its own checkout
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# MCP, pointed at it
./mvnw spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments="--automatiam.mcp.mocks.enabled=false \
    --automatiam.mcp.oneiam.base-url=http://localhost:8080/oneid-entitlement"
```

## Seeded documents

The seed uses fixed document IDs so rerunning it updates the same four records. It includes
snake_case and camelCase spellings for the application and elevated-access fields. OpenSearch
infers mappings from these local fixtures. Compatibility with a running Entitlement Manager
still requires an end-to-end check.

| Index | Id | Notes |
| --- | --- | --- |
| `managed_attributes_index` | `0abcfdb58d231b42818d28910e5d78b3` | `claims.adjuster` - elevated access, PHI + PFI |
| `managed_attributes_index` | `1bcdfdb58d231b42818d28910e5d78c4` | `claims.reader` - no elevated access, **no APM number**, which is the shape EM's `missingApm` metric counts |
| `application_index` | `APM001` | Claims Workbench |
| `identity_index` | `EF11111` | The owner both entitlements reference |

Everything here is invented. No identifier corresponds to a real entitlement, application or person.
