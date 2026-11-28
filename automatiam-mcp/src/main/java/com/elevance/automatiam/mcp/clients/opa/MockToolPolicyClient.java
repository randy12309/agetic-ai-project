package com.elevance.automatiam.mcp.clients.opa;

import com.elevance.automatiam.mcp.config.MockProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Synthetic policy decisions for local development and tests, mirroring
 * {@code deploy/opa/policy/tools.rego} closely enough to develop against.
 *
 * <p>Restricted to the local and test profiles and gated behind {@link MockProp […]
 * [@link Primary} so it displaces the real client when it is present at all.
 *
 * <p>Keep this in step with the sample Rego. Where they drift, the Rego is righ […]
 * the real bundle takes.
 */
@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "policy-enabled",
        havingValue = "true", matchIfMissing = true)
public class MockToolPolicyClient implements ToolPolicyClient {

    static final String POLICY_VERSION = "mock-0";

    /**
     * Group prefixes, not exact names: estate groups carry an environment suffi […]
     * ({@code -DEVROLE}, {@code -UATROLE}, {@code -PRODROLE]), and a rule writt […]
     * environment's spelling would silently stop matching in the next.
     */
    static final String FULL_ADMIN_PREFIX = "ONEIAM-FULLADMIN";
    static final String READONLY_ADMIN_PREFIX = "ONEIAM-READONLYADMIN";
    static final String ENTITLEMENT_GOV_PREFIX = "ONEIAM-ENTITLEMENTGOV";

    @Override
    public Mono<ToolAuthorizationDecision> authorize(ToolAuthorizationRequest request) {
        return Mono.fromSupplier(() -> decide(request));
    }

    private static ToolAuthorizationDecision decide(ToolAuthorizationRequest request) {
        return switch (request.targetType()) {
            case IDENTITY -> decideIdentity(request);
            case ENTITLEMENT, APPLICATION, SERVICE_IDENTITY, POLICY -> decideCatalogue(request);
        };
    }

    private static ToolAuthorizationDecision decideCatalogue(ToolAuthorizationRequest request) {
        boolean ownership = hasPrefix(request.groups(), FULL_ADMIN_PREFIX)
                || hasPrefix(request.groups(), ENTITLEMENT_GOV_PREFIX);
        return new ToolAuthorizationDecision(true, "catalogue_read", POLICY_VERSION,
                ownership ? List.of() : List.of(PolicyCondition.WITHHOLD_OWNERSHIP.wireName()));
    }


    /**
     * Reading another employee's access needs an admin group; reading your own […]
     * with no subject on the token is neither, so it is refused.
     */
    private static ToolAuthorizationDecision decideIdentity(ToolAuthorizationRequest request) {
        if (request.selfService()) {
            return new ToolAuthorizationDecision(true, "self_service", POLICY_VERSION, List.of());
        }
        if (hasPrefix(request.groups(), FULL_ADMIN_PREFIX)
                || hasPrefix(request.groups(), READONLY_ADMIN_PREFIX)) {
            return new ToolAuthorizationDecision(true, "delegated_identity_read", POLICY_VERSION,
                    List.of(PolicyCondition.AUDIT_REQUIRED.wireName()));
        }
        return ToolAuthorizationDecision.deny("cross_identity_access_denied", POLICY_VERSION);
    }

    private static boolean hasPrefix(Set<String> groups, String prefix) {
        return groups.stream().anyMatch(group -> group.startsWith(prefix));
    }
}
