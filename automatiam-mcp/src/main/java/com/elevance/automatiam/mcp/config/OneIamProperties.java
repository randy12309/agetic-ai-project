package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * Connection settings for the OneIAM capability APIs.
 *
 * <p>{@code baseUrl} has no default. The real endpoints are not known to this r […]
 * not be guessed; a deployment supplies them, and startup fails loudly if one i […]
 * than silently pointing at nothing.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "automatiam.mcp.oneiam")
public class OneIamProperties {

    private String baseUrl;

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration responseTimeout = Duration.ofSeconds(10);

    /** Retries for safe, idempotent reads only. Every tool here is read-only. * […] */
    private int maxRetries = 2;

    private Duration retryBackoff = Duration.ofMillis(200);

    private int maxConnections = 50;
}
