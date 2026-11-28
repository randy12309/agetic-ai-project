package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ManagerMetrics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
public class MockManagerMetricsClient implements ManagerMetricsClient {

    /**
     * Returns a dashboard for whoever is asked about. A manager with no entitle […]
     * all zeroes rather than not-found: "you manage nothing" is a valid answer, […]
     * leak whether a domain ID is a manager at all.
     */
    @Override
    public Mono<ManagerMetrics> metricsFor(String managerDomainId) {
        return Mono.fromSupplier(() -> {
            if (SyntheticIamData.SELF_DOMAIN_ID.equalsIgnoreCase(managerDomainId)) {
                return SyntheticIamData.MANAGER_METRICS;
            }
            return new ManagerMetrics(managerDomainId, 0L, 0L, 0L, 0L);
        });
    }
}
