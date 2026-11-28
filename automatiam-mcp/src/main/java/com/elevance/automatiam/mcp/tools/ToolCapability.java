package com.elevance.automatiam.mcp.tools;

import com.elevance.automatiam.mcp.clients.opa.ToolAuthorizationDecision;
import com.elevance.automatiam.mcp.security.ExecutionContext;

import reactor.core.publisher.Mono;

/**
 * The OneIAM call a tool makes once policy has permitted it.
 *
 * <p>Receives the policy decision as well as the context so a capability can ho […]
 * policy attached to the permit, such as withholding ownership metadata.
 */
@FunctionalInterface
public interface ToolCapability<A, T> {

    Mono<T> invoke(A arguments, ExecutionContext context, ToolAuthorizationDecision decision);
}
