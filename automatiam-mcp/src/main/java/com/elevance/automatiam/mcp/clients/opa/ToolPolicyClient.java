package com.elevance.automatiam.mcp.clients.opa;

import com.elevance.automatiam.mcp.exception.PolicyUnavailableException;

import reactor.core.publisher.Mono;

/**
 * The policy decision point for tool-level authorization. OPA runs as a sidecar […]
 * decision; this server supplies facts and applies the answer.
 *
 * <p>Policy domain {@code automatiam.tools}. Authoritative Rego lives in {@code […]
 * and reaches the sidecar as a signed bundle; what ships in {@code deploy/opa) […]
 * sample.
 *
 * <p>Implementations must fail closed: an unreachable sidecar, a malformed answ […]
 * policy yields {@link PolicyUnavailableException), never a default permit.
 */
public interface ToolPolicyClient {

    Mono<ToolAuthorizationDecision> authorize(ToolAuthorizationRequest request);
}
