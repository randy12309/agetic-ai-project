package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Refuses to start with synthetic adapters outside local and test.
 *
 * <p>The profile condition on the mock beans already prevents this. The guard e […]
 * failure mode it covers - a deployed environment quietly answering from synthe […]
 * authenticating nobody - is not one to leave to a single annotation.
 */
@Component
public class MockGuard implements ApplicationListener<ApplicationReadyEvent> {

    private static final Set<String> MOCK_SAFE_PROFILES = Set.of("local", "test");

    private final Environment environment;
    private final MockProperties mockProperties;

    public MockGuard(Environment environment, MockProperties mockProperties) {
        this.environment = environment;
        this.mockProperties = mockProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!mockProperties.isEnabled()) {
            return;
        }
        List<String> active = List.of(environment.getActiveProfiles());
        if (active.stream().noneMatch(MOCK_SAFE_PROFILES::contains)) {
            throw new IllegalStateException(
                    "automatiam.mcp.mocks.enabled is true but no local/test profile is active; "
                            + "active profiles: " + active);
        }
    }
}
