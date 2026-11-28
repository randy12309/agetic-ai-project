package com.elevance.automatiam.mcp.model;

import java.util.List;
import java.util.Optional;

/**
 * The result of {@code evaluate_policy}.
 *
 * <p>Advisory only. {@code advisory} is fixed true and serialized on every resp […]
 * that ignores documentation still sees that nothing was authorized or performe […]
 * answer to "what would policy say", never a grant.
 *
 * @param effect       what policy would decide
 * @param policyId     the approved policy that was evaluated
 * @param policyVersion version of the policy bundle that decided
 * @param reasonCodes  stable machine-readable reasons, safe to log and branch o […]
 * @param conditions   requirements that would have to hold for an ALLOW, where […]
 * @param explanation  human-readable rationale for an operator; not a contract […]
 */
public record PolicySimulation(
        PolicyEffect effect,
        String policyId,
        String policyVersion,
        List<String> reasonCodes,
        List<String> conditions,
        Optional<String> explanation,
        boolean advisory) {

    public PolicySimulation {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        explanation = explanation == null ? Optional.empty() : explanation;
    }

    public static PolicySimulation advisory(
            PolicyEffect effect,
            String policyId,
            String policyVersion,
            List<String> reasonCodes,
            List<String> conditions,
            String explanation) {
        return new PolicySimulation(
                effect, policyId, policyVersion, reasonCodes, conditions,
                Optional.ofNullable(explanation), true);
    }
}
