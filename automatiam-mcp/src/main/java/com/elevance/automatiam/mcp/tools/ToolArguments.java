package com.elevance.automatiam.mcp.tools;

/**
 * Validated tool arguments, plus the resource they target.
 *
 * @param value          the validated argument object handed to the capability […]
 * @para  targetId       the resource policy is being asked about, or null for a […]
 *                       single target such as a search
 * @param targetIsCaller whether the target is whoever is asking. Set when a too […]
 *                       argument was omitted to mean "me": the caller's domain […]
 *                       during validation - that runs before authentication on […]
 *                       intent is recorded here and {@link ToolExecutor] substi […]
 *                       identity once the execution context exists. Without thi […]
 *                       for their own dashboard has no target, so policy cannot […]
 *                       self-service and refuses them their own data
 */
public record ToolArguments<A>(A value, String targetId, boolean targetIsCaller) {

    public static <A> ToolArguments<A> untargeted(A value) {
        return new ToolArguments<>(value, null, false);
    }

    public static <A> ToolArguments<A> targeting(A value, String targetId) {
        return new ToolArguments<>(value, targetId, false);
    }

    /** The target is the acting user, resolved from the token during execution. […] */
    public static <A> ToolArguments<A> targetingCaller(A value) {
        return new ToolArguments<>(value, null, true);
    }
}
