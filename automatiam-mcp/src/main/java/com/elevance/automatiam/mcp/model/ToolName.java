package com.elevance.automatiam.mcp.model;

/**
 * The tools this server exposes. The wire name is the MCP tool identifier and t […]
 * so it is declared once here rather than repeated as a string literal at each […]
 */
public enum ToolName {

    SEARCH_ENTITLEMENTS("search_entitlements"),
    GET_ENTITLEMENT_METADATA("get_entitlement_metadata"),
    GET_ENTITLEMENT_DETAIL("get_entitlement_detail"),
    GET_MANAGER_METRICS("get_manager_metrics"),
    LOOKUP_APPLICATION("lookup_application"),
    GET_IDENTITY_ACCESS("get_identity_access"),
    GET_SERVICE_ID_METADATA("get_service_id_metadata"),
    EVALUATE_POLICY("evaluate_policy");

    private final String wireName;

    ToolName(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
