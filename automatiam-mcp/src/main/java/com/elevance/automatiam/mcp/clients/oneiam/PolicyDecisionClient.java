package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.PolicySimulation;
import com.elevance.automatiam.mcp.model.PolicySimulationRequest;

import reactor.core.publisher.Mono;

/**
 * Evaluates a proposed IAM decision without performing it.
 *
 * <p>The shipped implementation is OPA-backed
 * ({@link com.elevance.automatiam.mcp.clients.opa.OpaPolicyDecisionClient]) and […]
 * policy from the {@code automatiam.tools} namespace. The interface sits with t […]
 * because the capability is OneIAM's; where it is served from is an implementat […]
 *
 * <p>Implementations must never perform the proposed action.
 */
public interface PolicyDecisionClient {

    Mono<PolicySimulation> simulate(PolicySimulationRequest request);
}
