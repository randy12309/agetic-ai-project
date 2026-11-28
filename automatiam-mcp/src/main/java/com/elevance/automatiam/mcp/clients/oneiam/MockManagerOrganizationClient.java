package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ManagerOrganization;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
public class MockManagerOrganizationClient implements ManagerOrganizationClient {

    @Override
    public Mono<ManagerOrganization> organizationFor(String managerDomainId) {
        return Mono.fromSupplier(() -> {
            if (SyntheticIamData.SELF_DOMAIN_ID.equalsIgnoreCase(managerDomainId)) {
                return new ManagerOrganization(managerDomainId, true, 12);
            }
            return new ManagerOrganization(managerDomainId, false, 0);
        });
    }
}
