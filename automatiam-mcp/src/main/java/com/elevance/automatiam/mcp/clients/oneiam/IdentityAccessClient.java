package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.IdentityAccessResult;
import com.elevance.automatiam.mcp.model.Pagination;

import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Retrieves an identity's entitlement assignments.
 *
 * <p>The most sensitive adapter here. The upstream service applies its own auth […]
 * remains authoritative; the decision this server's policy made does not substi […]
 *
 * <p>{@code domainId} is an Elevance Health domain ID - the network login ident […]
 * by {@code DomainIdResolver} in {@code oneiam-shared-models]. It is not the Ok […]
 * only the last-resort fallback in the estate's resolution order.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * its pagination contract, and confirmation of which assignment fields are appr […]
 * disclosure. The response must not include credentials or personal data beyond […]
 * itself; if it does, this adapter filters before returning.
 */
public interface IdentityAccessClient {

    Mono<IdentityAccessResult> accessFor(
            String domainId, Optional<String> applicationId, Pagination pagination, boolean selfService);
}
