package com.elevance.automatiam.mcp.model;

/**
 * Resource kinds a policy simulation may target. A closed set: an unrecognised […]
 * rather than passed through to policy, so the simulation surface cannot be wid […]
 */
public enum TargetResourceType {

    APPLICATION,
    ENTITLEMENT,
    SERVICE_IDENTITY;

    public static TargetResourceType parse(String value) {
        return EnumParser.parse(TargetResourceType.class, "resourceType", value);
    }
}
