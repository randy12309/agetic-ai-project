package com.elevance.automatiam.mcp.model;

import java.util.List;

/**
 * An identity's authorized entitlement assignments.
 *
 * @param domainId the domain ID the assignments belong to, echoed so a caller h […]
 *                 in-flight requests cannot misattribute a result
 * @param selfService whether the caller was asking about themselves; the orches […]
 *                   this to choose wording, and it makes the distinction visibl […]
 */
public record IdentityAccessResult(
        String domainId,
        boolean selfService,
        List<EntitlementAssignment> assignments,
        PageMetadata page) {

    public IdentityAccessResult {
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }
}
