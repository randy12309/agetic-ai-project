package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * Connection settings for the OPA sidecar.
 *
 * <p>The default base URL is loopback. OPA listens inside the pod only and is n […]
 * through the Service, so nothing outside the pod can query or influence policy […]
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "automatiam.mcp.opa")
public class OpaProperties {

    private String baseUrl = "http://127.0.0.1:8181";

    /** Document path for tool-level authorization decisions. */
    private String authorizationPath = "/v1/data/automatiam/tools/authz";

    /** Document path for advisory policy simulation. */
    private String simulationPath = "/v1/data/automatiam/tools/simulate";

    /**
     * Deadline for a policy decision. Short by design: policy runs in-pod, and […]
     * must fail closed rather than hold a tool call open.
     */
    private Duration timeout = Duration.ofSeconds(2);
}
