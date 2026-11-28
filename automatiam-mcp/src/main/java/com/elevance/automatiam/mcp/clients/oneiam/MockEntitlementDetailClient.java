package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.exception.ResourceNotFoundException;
import com.elevance.automatiam.mcp.model.EntitlementDetail;
import com.elevance.automatiam.mcp.model.EntitlementOwnership;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
public class MockEntitlementDetailClient implements EntitlementDetailClient {

    @Override
    public Mono<EntitlementDetail> findById(String entitlementId, boolean includeOwnership) {
        return SyntheticIamData.DETAILS.stream()
                .filter(detail -> detail.entitlementId().equals(entitlementId))
                .findFirst()
                .map(detail -> withOwnership(detail, includeOwnership))
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new ResourceNotFoundException("entitlement")));
    }

    private static EntitlementDetail withOwnership(EntitlementDetail source, boolean include) {
        if (include) {
            return source;
        }
        return new EntitlementDetail(
                source.entitlementId(), source.displayName(), source.description(),
                source.applicationName(), source.apmNumber(), source.platform(),
                source.environment(), source.status(), source.elevatedAccess(),
                source.classifications(), source.memberCount(), source.locationRestrictions(),
                source.targetedUserGroup(), Optional.<EntitlementOwnership>empty());
    }
}
