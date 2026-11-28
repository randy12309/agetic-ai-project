package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.EntitlementMetadata;

import reactor.core.publisher.Mono;

/**
 * Retrieves metadata for a single entitlement.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * the response field names, which classification attributes are approved for di […]
 * response signals "no such entitlement" versus "not visible to you" - the two […]
 * distinguishable even though the tool collapses them before answering.
 *
 * @param includeOwnership whether policy authorized ownership disclosure for th […]
 */
public interface EntitlementMetadataClient {

    Mono<EntitlementMetadata> findById(String entitlementId, boolean includeOwnership);
}
