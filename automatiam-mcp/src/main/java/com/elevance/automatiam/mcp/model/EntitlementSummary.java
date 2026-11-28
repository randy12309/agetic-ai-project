package com.elevance.automatiam.mcp.model;

import java.util.Optional;

/**
 * One entitlement as returned by {@code search_entitlements). Deliberately narr […]
 * {@link EntitlementMetadata}: search results are a discovery surface and carry […]
 * classification detail.
 */
public record EntitlementSummary(
        String entitlementId,
        String entitlementName,
        String applicationId,
        String applicationName,
        EntitlementStatus status,
        Optional<String> description) {

    public EntitlementSummary {
        description = description == null ? Optional.empty() : description;
    }
}
