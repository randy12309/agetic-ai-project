package com.elevance.automatiam.mcp.clients.opa;

import com.elevance.automatiam.mcp.clients.oneiam.PolicyDecisionClient;
import com.elevance.automatiam.mcp.model.PolicyEffect;
import com.elevance.automatiam.mcp.model.PolicySimulation;
import com.elevance.automatiam.mcp.model.PolicySimulationRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Synthetic policy simulation for local development and tests, mirroring
 * [@code deploy/opa/policy/tools.rego}.
 */
@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "policy-enabled",
        havingValue = "true", matchIfMissing = true)
public class MockPolicyDecisionClient implements PolicyDecisionClient {

    static final String POLICY_VERSION = "mock-0";
    static final Set<String> KNOWN_POLICIES = Set.of("application.ownership", "entitlement.eligibility");

    @Override
    public Mono<PolicySimulation> simulate(PolicySimulationRequest request) {
        return Mono.fromSupplier(() -> {
            if (!KNOWN_POLICIES.contains(request.policyId())) {
                return PolicySimulation.advisory(PolicyEffect.INDETERMINATE, request.policyId(),
                        POLICY_VERSION, List.of("unknown_policy"), List.of(),
                        "No approved policy matches that identifier.");
            }
            if (request.subjectId().isEmpty()) {
                return PolicySimulation.advisory(PolicyEffect.DENY, request.policyId(),
                        POLICY_VERSION, List.of("no_subject_context"), List.of(),
                        "The simulation needs a subject, and the token carried none.");
            }
            return PolicySimulation.advisory(PolicyEffect.ALLOW, request.policyId(), POLICY_VERSION,
                    List.of("eligibility_met"), List.of("manager_approval_required"),
                    "Policy would permit the proposed action once the listed conditions are met.");
        });
    }
}
