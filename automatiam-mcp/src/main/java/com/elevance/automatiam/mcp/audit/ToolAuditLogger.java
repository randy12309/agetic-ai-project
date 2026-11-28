package com.elevance.automatiam.mcp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

/**
 * Emits one structured record per tool execution.
 *
 * <p>Every field here is either an identifier the caller already holds, an enum […]
 * record carries no tool arguments, no tool response, no entitlement names and […]
 * identifiers: this stream lands in Splunk, which is not an access-controlled s […]
 * The authoritative record of who read what is written by the OneIAM capability […]
 * request, which remains the system of record.
 *
 * <p>Fields are attached as key-value pairs rather than interpolated into the m […]
 * Boot's structured logging renders them as discrete JSON fields.
 */
@Component
public class ToolAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("automatiam.mcp.audit");

    public void record(AuditEvent event) {
        LoggingEventBuilder builder = (event.outcome() == AuditOutcome.SUCCESS ? log.atInfo() : log.atWarn())
                .addKeyValue("event", "mcp.tool.execution")
                .addKeyValue("correlationId", event.correlationId())
                .addKeyValue("requestId", event.requestId())
                .addKeyValue("workload", event.workloadClientId())
                .addKeyValue("tool", event.tool().wireName())
                .addKeyValue("toolVersion", event.toolVersion())
                .addKeyValue("authorizationDecision", event.decision())
                .addKeyValue("targetType", event.targetType())
                .addKeyValue("subjectPresent", event.subjectPresent())
                .addKeyValue("subjectSource", event.subjectSource())
                .addKeyValue("selfService", event.selfService())
                .addKeyValue("durationMs", event.durationMillis())
                .addKeyValue("outcome", event.outcome())
                .addKeyValue("resultCount", event.resultCount());

        event.policyVersion().ifPresent(version -> builder.addKeyValue("policyVersion", version));
        event.delegatingActor().ifPresent(actor -> builder.addKeyValue("delegatingActor", actor));
        event.errorCategory().ifPresent(category -> builder.addKeyValue("errorCategory", category));
        event.reasonCode().ifPresent(reason -> builder.addKeyValue("reasonCode", reason));

        builder.log("mcp tool execution");
    }
}
