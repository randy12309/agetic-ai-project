package com.elevance.automatiam.mcp.tools.entitlement;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.EntitlementMetadataClient;
import com.elevance.automatiam.mcp.clients.opa.PolicyCondition;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.EntitlementMetadata;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

public class GetEntitlementMetadataTool {

    private final ToolExecutor executor;
    private final EntitlementMetadataClient metadataClient;

    public GetEntitlementMetadataTool(
            ToolExecutor executor, EntitlementMetadataClient metadataClient) {
        this.executor = executor;
        this.metadataClient = metadataClient;
    }

    @McpTool(
            name = "get_entitlement_metadata",
            title = "Get entitlement metadata",
            description = """
                    Retrieve metadata for one entitlement: name, description, owning application,
                    status, and ownership and classification attributes where the caller is authorized
                    to see them. Read-only.""")
    public Mono<EntitlementMetadata> getEntitlementMetadata(
            @McpToolParam(required = true,
                    description = "The entitlement identifier, for example ENT-2001.")
            String entitlementId) {

        return executor.execute(
                ToolName.GET_ENTITLEMENT_METADATA,
                TargetType.ENTITLEMENT,
                () -> {
                    String identifier = require(entitlementId);
                    return ToolArguments.targeting(identifier, identifier);
                },
                (identifier, context, decision) -> metadataClient.findById(
                        identifier, PolicyCondition.ownershipPermitted(decision)),
                result -> 1);
    }

    private static String require(String entitlementId) {
        if (entitlementId == null || entitlementId.isBlank()) {
            throw new InvalidToolArgumentException("entitlementId", "must not be blank");
        }
        return entitlementId.strip();
    }
}
