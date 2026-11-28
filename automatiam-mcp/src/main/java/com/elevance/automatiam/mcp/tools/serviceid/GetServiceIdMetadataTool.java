package com.elevance.automatiam.mcp.tools.serviceid;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.ServiceIdentityClient;
import com.elevance.automatiam.mcp.clients.opa.PolicyCondition;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.ServiceIdentityMetadata;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

public class GetServiceIdMetadataTool {

    private final ToolExecutor executor;
    private final ServiceIdentityClient serviceIdentityClient;

    public GetServiceIdMetadataTool(
            ToolExecutor executor, ServiceIdentityClient serviceIdentityClient) {
        this.executor = executor;
        this.serviceIdentityClient = serviceIdentityClient;
    }

    @McpTool(
            name = "get_service_id_metadata",
            title = "Get service identity metadata",
            description = """
                    Retrieve approved metadata for one service identity: associated application,
                    ownership, status and non-sensitive attributes. Never returns credentials,
                    tokens, certificates or private keys. Read-only.""")
    public Mono<ServiceIdentityMetadata> getServiceIdMetadata(
            @McpToolParam(required = true,
                    description = "The service identity identifier, for example SVC-3001.")
            String serviceId) {

        return executor.execute(
                ToolName.GET_SERVICE_ID_METADATA,
                TargetType.SERVICE_IDENTITY,
                () -> {
                    String identifier = require(serviceId);
                    return ToolArguments.targeting(identifier, identifier);
                },
                (identifier, context, decision) -> serviceIdentityClient.findById(
                        identifier, PolicyCondition.ownershipPermitted(decision)),
                result -> 1);
    }

    private static String require(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new InvalidToolArgumentException("serviceId", "must not be blank");
        }
        return serviceId.strip();
    }
}
