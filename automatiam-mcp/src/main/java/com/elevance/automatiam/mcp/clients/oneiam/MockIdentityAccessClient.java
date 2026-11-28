package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.EntitlementAssignment;
import com.elevance.automatiam.mcp.model.IdentityAccessResult;
import com.elevance.automatiam.mcp.model.Pagination;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
public class MockIdentityAccessClient implements IdentityAccessClient {

    @Override
    public Mono<IdentityAccessResult> accessFor(
            String domainId, Optional<String> applicationId, Pagination pagination, boolean selfService) {
        return Mono.fromSupplier(() -> {
            List<EntitlementAssignment> all = SyntheticIamData.ASSIGNMENTS
                    .getOrDefault(domainId, List.of()).stream()
                    .filter(assignment -> applicationId
                            .map(id -> id.equals(assignment.applicationId())).orElse(true))
                    .toList();

            List<EntitlementAssignment> slice = MockPaging.slice(all, pagination);
            return new IdentityAccessResult(
                    domainId, selfService, slice, MockPaging.metadata(all, slice, pagination));
        });
    }
}
