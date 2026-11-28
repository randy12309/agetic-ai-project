# OPA sidecar

OPA runs beside the MCP server in the same pod and listens on `127.0.0.1:8181` only - it is never
exposed through the Service, so nothing outside the pod can query or influence policy.

**This server gets its own sidecar (decided 2026-11-14).** OneIAM runs a central OPA, and that is
the right design for the platform. The sidecar here is not a disagreement with it - it exists to
isolate the agent authorization surface.

Agent tool authorization is new, changes on a different cadence, and has different reviewers than
platform authorization. Keeping it in its own sidecar means a change to what an AI agent may invoke
cannot reach platform authorization, and a mistake in platform policy cannot widen what an agent may
do. It also means this workload adds no new dependency to a service the rest of OneIAM relies on,
which matters while the agent surface is still unproven.

If the isolation argument stops holding - if agent tool policy becomes ordinary platform policy with
the same reviewers and cadence - then consolidating onto the central deployment is the correct move.
Revisit it on that basis, not on availability grounds.

`policy/tools.rego` is a **development sample**. The authoritative policy for `automatiam.tools`
lives in `automatiam-policies` and reaches the sidecar as a signed bundle. The sample is kept close
enough to the real shape to develop against, and `MockToolPolicyClient` mirrors it again in Java for
tests that must run without a sidecar.

Two decisions are delegated:

1. **`automatiam.tools.authz`** - may this workload invoke this tool against this resource, and
   must any output be narrowed (for example `withhold_ownership`).
2. **`automatiam.tools.simulate`** - the advisory answer behind `evaluate_policy`. It decides what
   policy *would* say; it never authorizes anything.

## Why the server, not the policy, decides `selfService`

`input.selfService` is computed by the server by comparing the token's delegation claim to the
identity being asked about. It is not a field a caller can set. Policy may rely on it precisely
because a workflow cannot forge it.

## Readiness must be gated on bundle load

The kubelet checks `/health?bundle=true` on port 8282 at the Pod IP. The Service exposes only the
application port. Configure the bundle publisher's verification key in the OPA ConfigMap before
deploying. The server fails closed when policy is
unavailable, so a pod that accepts traffic before its bundle has loaded answers `POLICY_UNAVAILABLE`
to every call in that window. The probe is what makes failing closed invisible during a rollout
instead of a brief outage on every deployment.

## Failing closed

If the sidecar is unreachable, or the document is not loaded, or it returns no `allow`, the server
raises `POLICY_UNAVAILABLE` and the tool does not run. That is deliberate over a default-allow: the
sidecar being down is not evidence that the call is safe.

Note the asymmetry - a missing `allow` is treated as *unavailability*, not as a deny. A broken or
unloaded bundle is an operational fault, and reporting it as a routine refusal would hide it.

## Running the sample locally

```bash
opa run --server --addr 127.0.0.1:8181 deploy/opa/policy
```

Then start the server with `--spring.profiles.active=local --automatiam.mcp.mocks.policy-enabled=false` to
exercise a real OPA instead of the in-process mock.

## Testing the policy itself

Rego unit tests belong with the policy, in `automatiam-policies`. What this repo tests is that the
server applies a decision correctly and fails closed when it cannot get one.
