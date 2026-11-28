package com.elevance.automatiam.mcp.tools.entitlement;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.EntitlementDetailClient;
import com.elevance.automatiam.mcp.clients.opa.PolicyCondition;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.EntitlementDetail;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

/**
 * Backs the screen-aware explainer: everything EM shows on its entitlement deta […]
 * shape an assistant can turn into plain English.
 *
 * <p>Takes an entitlement id and nothing else. When the caller is looking at an […]
 * the id is pre-filled from page context - but the authorization is identical t […]
 * it, because page context reaches this server as a tool argument and arguments […]
 * authority.
 */
public class GetEntitlementDetailTool {

    private final ToolExecutor executor;
    private final EntitlementDetailClient detailClient;

    public GetEntitlementDetailTool(ToolExecutor executor, EntitlementDetailClient detailClient) {
        this.executor = executor;
        this.detailClient = detailClient;
    }

    @McpTool(
            name = "get_entitlement_detail",
            title = "Explain an entitlement",
            description = """
                    Retrieve the full detail of one entitlement so it can be explained in plain
                    language: what it grants, its application and APM number, whether it gives
                    elevated access, which data classifications it reaches (PHI, PFI, PI, PII, CPI),
                    how many identities hold it, location and user-group restrictions, and ownership
                    where the caller is authorized to see it. Read-only.""")
    public Mono<EntitlementDetail> getEntitlementDetail(
            @McpToolParam(required = true,
                    description = "The entitlement identifier, for example ENT-2001.")
            String entitlementId) {

        return executor.execute(
                ToolName.GET_ENTITLEMENT_DETAIL,
                TargetType.ENTITLEMENT,
                () -> {
                    String identifier = require(entitlementId);
                    return ToolArguments.targeting(identifier, identifier);
                },
                (identifier, context, decision) -> detailClient.findById(
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
