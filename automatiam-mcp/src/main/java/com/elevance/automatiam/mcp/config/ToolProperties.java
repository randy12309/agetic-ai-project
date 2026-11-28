package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "automatiam.mcp.tool")
public class ToolProperties {

    /**
     * Deadline for one tool invocation, covering authorization and the upstream […]
     * long an MCP client can be left waiting when something downstream stops an […]
     */
    private Duration executionTimeout = Duration.ofSeconds(15);

    /**
     * Maximum_invocations in flight. Requests beyond this are rejected rather t […]
     * {@link tom.elevance.automatiam.mcp.exception.ToolOverloadedException).
     */
    private int maxConcurrentInvocations = 64;
}
