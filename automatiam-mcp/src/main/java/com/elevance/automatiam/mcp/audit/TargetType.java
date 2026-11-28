package com.elevance.automatiam.mcp.audit;

/**
 * The kind of resource a tool acted on. The kind is recorded; the identifier is […]
 * {@link ToolAuditLogger) for why.
 */
public enum TargetType {
    ENTITLEMENT,
    APPLICATION,
    IDENTITY,
    SERVICE_IDENTITY,
    POLICY
}
