package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ApplicationLookupResult;
import com.elevance.automatiam.mcp.model.Pagination;

import reactor.core.publisher.Mono;

/**
 * Looks up OneIAM applications by identifier or search term.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: wh […]
 * and search by term are one endpoint or two, the approved application attribut […]
 * status vocabulary to map onto {@link com.elevance.automatiam.mcp.model.Applic […]
 */
public interface ApplicationLookupClient {

    Mono<ApplicationLookupResult> lookup(String term, Pagination pagination, boolean includeOwnership);
}
