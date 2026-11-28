package com.elevance.automatiam.mcp.model;

/**
 * Governance counts for one manager's entitlements, mirroring EM's
 * {@code ManagerDashboardMetricsDto).
 *
 * <p>Counts only. The tool reports how many entitlements are missing informatio […]
 * ones is {@code search_entitlements} with a filter, authorized separately. Kee […]
 * means a manager's summary cannot become a bulk export of their org's access.
 *
 * @param managerDomainId   whose dashboard this is, echoed so a result cannot b […]
 * @param totalEntitlements entitlements in this manager's scope
 * @param missingInformation entitlements lacking required metadata
 * @param missingApm        entitlements with no APM number recorded
 * @param pendingTransfers  entitlements awaiting an ownership transfer
 */
public record ManagerMetrics(
        String managerDomainId,
        long totalEntitlements,
        long missingInformation,
        long missingApm,
        long pendingTransfers) {

    /** Whether anything on this dashboard warrants attention, for a one-line an […] */
    public boolean hasGovernanceGaps() {
        return missingInformation > 0 || missingApm > 0 || pendingTransfers > 0;
    }
}
