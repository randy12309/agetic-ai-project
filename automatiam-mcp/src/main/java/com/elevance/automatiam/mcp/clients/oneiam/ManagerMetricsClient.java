package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ManagerMetrics;

import reactor.core.publisher.Mono;

/**
 * Retrieves governance counts for one manager's entitlement scope, backing EM's […]
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * {@code POST /api/v1/manager-dashboard/metrics}, and whether the filter dimens […]
 * (platform, environment, DPI, classification, date range) should be exposed th […]
 * fixed. They are deliberately not exposed yet - an unfiltered count is a summa […]
 * filtered one starts to become a query interface over an org's access.
 *
 * <p>The upstream service resolves scope from the manager's domain ID. This ada […]
 * it was given; it does not decide whose dashboard may be read, which is OPA's […]
 */
public interface ManagerMetricsClient {
    Mono<ManagerMetrics> metricsFor(String managerDomainId);
}
