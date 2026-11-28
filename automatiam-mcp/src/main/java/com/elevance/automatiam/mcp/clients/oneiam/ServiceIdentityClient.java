package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ServiceIdentityMetadata;

import reactor.core.publisher.Mono;

/**
 * Retrieves approved metadata for a service identity.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * endpoint, the approved non-sensitive attribute set, and written confirmation […]
 * fields carry secret material. Any such field must be dropped in this adapter […]
 * onto {@link ServiceIdentityMetadata}, which has nowhere to put it.
 */
public interface ServiceIdentityClient {

    Mono<ServiceIdentityMetadata> findById(String serviceId, boolean includeOwnership);
}
