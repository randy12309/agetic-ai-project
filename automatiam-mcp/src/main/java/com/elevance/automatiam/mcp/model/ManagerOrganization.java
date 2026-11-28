package com.elevance.automatiam.mcp.model;

/**
 * A manager's organization, from the identity management service.
 *
 * <p>A count of reports, never the roster. {@code identity/{id]/organization) r […]
 * list of identities; this server counts them and discards the rest. Naming the […]
 * to someone is a different disclosure, and if a workflow needs it, it should a […]
 * tool authorized for that purpose.
 *
 * @param isManager        from IMS {@code managerStatus]. False for someone wit […]
 *                         is a valid answer rather than an error
 * @param directReportCount how many non-terminated identities report to them. I […]
 *                         terminated identities from the organization view
 */
public record ManagerOrganization(String managerDomainId, boolean isManager, int directReportCount) {
}
