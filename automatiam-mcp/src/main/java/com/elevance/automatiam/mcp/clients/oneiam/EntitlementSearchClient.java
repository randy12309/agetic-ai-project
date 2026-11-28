package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.EntitlementSearchCriteria;
import com.elevance.automatiam.mcp.model.EntitlementSearchResult;

import reactor.core.publisher.Mono;

/**
 * Searches the OneIAM entitlement catalogue.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * endpoint and method, its query and pagination parameter names, the response f […]
 * entitlement ids name, application and status, the status vocabulary to map on […]
 * {@link com.elevance.automatiam.mcp.model.EntitlementStatus], and how the serv […]
 * caller's identity presented so it can apply its own authorization.
 */
public interface EntitlementSearchClient {

    /**
     * @return entitlements matching the criteria that the caller is authorized […]
     *         paginated by the upstream service
     */
    Mono<EntitlementSearchResult> search(EntitlementSearchCriteria criteria);
}
