package com.elevance.automatiam.mcp.clients.opa;

import java.util.List;

/**
 * Conditions policy can attach to a permit.
 *
 * <p>A closed vocabulary shared with the Rego bundle. A condition the server do […]
 * ignored for output shaping, so adding one to policy cannot silently widen wha […]
 * widening requires a change here as well.
 */
public enum PolicyCondition {

    /** Return the record, but without ownership metadata. */
    WITHHOLD_OWNERSHIP("withhold_ownership"),

    /** The permit is conditional on the invocation being audited, which it alwa […] */
    AUDIT_REQUIRED("audit_required");

    private final String wireName;

    PolicyCondition(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public boolean presentIn(List<String> conditions) {
        return conditions != null && conditions.contains(wireName);
    }

    /** Whether policy permitted ownership metadata to be returned. */
    public static boolean ownershipPermitted(ToolAuthorizationDecision decision) {
        return !WITHHOLD_OWNERSHIP.presentIn(decision.conditions());
    }
}
