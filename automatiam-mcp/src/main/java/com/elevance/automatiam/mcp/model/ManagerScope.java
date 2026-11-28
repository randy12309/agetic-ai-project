package com.elevance.automatiam.mcp.model;

/**
 * What {@code get_manager_metrics) returns: a manager's organization from the i […]
 * service, combined with their entitlement governance counts from the entitleme […]
 *
 * <p>Composed from two services rather than computed here. Deriving the entitle […]
 * raw identity data would mean reimplementing the aggregation EM already owns, […]
 * of business rule this server is explicitly not supposed to hold.
 *
 * <p>Flat rather than nested because an assistant narrates it as one answer; th […]
 * the provenance that the shape does not.
 *
 * @param isManager         IMS {@code managerStatus)
 * @param directReportCount IMS organization size, terminated identities exclude […]
 * @param totalEntitlements EM: entitlements in this manager's scope
 * @param missingInformation EM: entitlements lacking required metadata
 * @param missingApm        EM: entitlements with no APM number recorded
 * @param pendingTransfers  EM: entitlements awaiting an ownership transfer
 */
public record ManagerScope(
        String managerDomainId,
        boolean isManager,
        int directReportCount,
        long totalEntitlements,
        long missingInformation,
        long missingApm,
        long pendingTransfers) {

    public static ManagerScope of(ManagerOrganization organization, ManagerMetrics metrics) {
        return new ManagerScope(
                organization.managerDomainId(),
                organization.isManager(),
                organization.directReportCount(),
                metrics.totalEntitlements(),
                metrics.missingInformation(),
                metrics.missingApm(),
                metrics.pendingTransfers());
    }

    /** Whether anything here warrants attention, for a one-line answer. */
    public boolean hasGovernanceGaps() {
        return missingInformation > 0 || missingApm > 0 || pendingTransfers > 0;
    }
}
