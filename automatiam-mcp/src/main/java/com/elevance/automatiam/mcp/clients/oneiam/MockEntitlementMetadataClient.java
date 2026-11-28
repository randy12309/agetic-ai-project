package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.exception.ResourceNotFoundException;
import com.elevance.automatiam.mcp.model.EntitlementMetadata;

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
public class MockEntitlementMetadataClient implements EntitlementMetadataClient {

    @Override
    public Mono<EntitlementMetadata> findById(String entitlementId, boolean includeOwnership) {
        return SyntheticIamData.ENTITLEMENTS.stream()
                .filter(entitlement -> entitlement.entitlementId().equals(entitlementId))
                .findFirst()
                .map(entitlement -> withOwnership(entitlement, includeOwnership))
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new ResourceNotFoundException("entitlement")));
    }

    private static EntitlementMetadata withOwnership(EntitlementMetadata source, boolean include) {
        if (include) {
            return source;
        }
        return new EntitlementMetadata(source.entitlementId(), source.entitlementName(),
                source.description(), source.applicationId(), source.applicationName(),
                source.status(), Optional.empty(), source.attributes());
    }
}
