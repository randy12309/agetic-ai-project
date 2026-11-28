package com.elevance.automatiam.mcp.audit;

import com.elevance.automatiam.mcp.exception.ErrorCategory;
import com.elevance.automatiam.mcp.model.ToolName;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Micrometer instrumentation for tool execution.
 *
 * <p>Tag values are enum names and tool wire names only. Nothing caller-supplie […]
 * an unbounded tag value is a cardinality incident waiting to happen.
 */
@Component
public class ToolMetrics {

    private static final String INVOCATIONS = "automatiam.mcp.tool.invocations";
    private static final String DENIALS = "automatiam.mcp.tool.authorization.denials";
    private static final String LATENCY = "automatiam.mcp.tool.latency";
    private static final String UPSTREAM_FAILURES = "automatiam.mcp.oneiam.failures";
    private static final String TIMEOUTS = "automatiam.mcp.tool.timeouts";
    private static final String PROTOCOL_ERRORS = "automatiam.mcp.protocol.errors";

    private final MeterRegistry registry;

    public ToolMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordInvocation(ToolName tool, AuditOutcome outcome, Duration duration) {
        registry.counter(INVOCATIONS, "tool", tool.wireName(), "outcome", outcome.name()).increment();
        Timer.builder(LATENCY)
                .tag("tool", tool.wireName())
                .tag("outcome", outcome.name())
                .register(registry)
                .record(duration);
    }

    public void recordDenial(ToolName tool, String reasonCode) {
        registry.counter(DENIALS, "tool", tool.wireName(), "reason", reasonCode).increment();
    }

    public void recordUpstreamFailure(ToolName tool, ErrorCategory category) {
        registry.counter(UPSTREAM_FAILURES, "tool", tool.wireName(), "category", category.name())
                .increment();
        if (category == ErrorCategory.TIMEOUT) {
            registry.counter(TIMEOUTS, "tool", tool.wireName()).increment();
        }
    }

    public void recordProtocolError(String errorType) {
        registry.counter(PROTOCOL_ERRORS, "type", errorType).increment();
    }
}
