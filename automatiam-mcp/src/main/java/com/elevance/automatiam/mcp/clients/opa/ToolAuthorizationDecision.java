package com.elevance.automatiam.mcp.clients.opa;

import java.util.List;

/**
 * What policy decided about one tool invocation.
 *
 * @param permitted    whether the invocation may proceed
 * @param reasonCode   stable machine-readable reason, safe to log and to return […]
 * @param policyVersion version of the bundle that decided, recorded on the audi […]
 * @param conditions   requirements policy attached to a permit, where it disclo […]
 */
public record ToolAuthorizationDecision(
        boolean permitted, String reasonCode, String policyVersion, List<String> conditions) {

    public ToolAuthorizationDecision {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public static ToolAuthorizationDecision deny(String reasonCode, String policyVersion) {
        return new ToolAuthorizationDecision(false, reasonCode, policyVersion, List.of());
    }
}
