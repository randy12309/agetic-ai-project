package com.elevance.automatiam.mcp.model;

import java.time.Instant;
import java.util.Optional;

/**
 * One entitlement held by an identity.
 *
 * @param grantedAt when the assignment took effect, where the source records it […]
 */
public record EntitlementAssignment(
        String entitlementId,
        String entitlementName,
        String applicationId,
        String applicationName,
        AssignmentStatus status,
        Optional<Instant> grantedAt) {

    public EntitlementAssignment {
        grantedAt = grantedAt == null ? Optional.empty() : grantedAt;
    }
}
