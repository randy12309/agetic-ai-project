package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Refuses to start outside local and test with peer subject assertion enabled.
 *
 * <p>ADR-010 selects RFC B693 token exchange. The assertion path survives only […]
 * should token exchange prove unavailable, and a contingency that can be switch […]
 * deployed environment is indistinguishable from the design. This guard makes e […]
 * startup failure rather than a configuration choice, so adopting it requires c […]
 * passing review - not editing a value.
 */
@Component
public class SubjectAssertionGuard implements ApplicationListener<ApplicationReadyEvent> {

    private static final Set<String> ASSERTION_SAFE_PROFILES = Set.of("local", "test");

    private final Environment environment;
    private final AuthenticationProperties authentication;

    public SubjectAssertionGuard(Environment environment, AuthenticationProperties authentication) {
        this.environment = environment;
        this.authentication = authentication;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!authentication.getSubjectAssertion().isEnabled()) {
            return;
        }
        List<String> active = List.of(environment.getActiveProfiles());
        if (active.stream().noneMatch(ASSERTION_SAFE_PROFILES::contains)) {
            throw new IllegalStateException(
                    "automatiam.mcp.auth.subject-assertion.enabled is true outside local/test. "
                            + "ADR-010 selects RFC 8693 token exchange; peer assertion is a "
                            + "contingency and must not be the deployed mechanism. Active profiles: "
                            + active);
        }
    }
}
