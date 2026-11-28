package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Switch for the synthetic OneIAM and policy adapters.
 *
 * <p>Defaults to false. The mock beans additionally require the {@code local] o […]
 * profile, so enabling this flag alone in a deployed environment does not bring […]
 * {@link MockGuard}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "automatiam.mcp.mocks")
public class MockProperties {

    private boolean enabled = false;
}
