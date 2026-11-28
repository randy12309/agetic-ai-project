package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.exception.ResourceNotFoundException;
import com.elevance.automatiam.mcp.model.ServiceIdentityMetadata;

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
public class MockServiceIdentityClient implements ServiceIdentityClient {

    @Override
    public Mono<ServiceIdentityMetadata> findById(String serviceId, boolean includeOwnership) {
        return SyntheticIamData.SERVICE_IDENTITIES.stream()
                .filter(identity -> identity.serviceId().equals(serviceId))
                .findFirst()
                .map(identity -> withOwnership(identity, includeOwnership))
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new ResourceNotFoundException("service identity")));
    }

    private static ServiceIdentityMetadata withOwnership(
            ServiceIdentityMetadata source, boolean include) {
        if (include) {
            return source;
        }
        return new ServiceIdentityMetadata(source.serviceId(), source.serviceName(),
                source.applicationId(), source.applicationName(), source.status(),
                Optional.empty(), source.attributes());
    }
}
