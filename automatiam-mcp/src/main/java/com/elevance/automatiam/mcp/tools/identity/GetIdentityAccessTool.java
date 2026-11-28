package com.elevance.automatiam.mcp.tools.identity;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.IdentityAccessClient;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.IdentityAccessResult;
import com.elevance.automatiam.mcp.model.Pagination;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * The most sensitive tool here.
 *          I
 * cp>{@code domainId} names the person being asked about; it never establishes […]
 * That comes from the validated token, so a workflow cannot read another employ […]
 * claiming to be them. Whether the two may differ is OPA's decision, and the On […]
 * enforces its own authorization on top.
 */
public class GetIdentityAccessTool {

    private final ToolExecutor executor;
    private final IdentityAccessClient accessClient;

    public GetIdentityAccessTool(ToolExecutor executor, IdentityAccessClient accessClient) {
        this.executor = executor;
        this.accessClient = accessClient;
    }

    @McpTool(
            name = "get_identity_access",
            title = "Get an identity's access",
            description = """
                    List the entitlement assignments held by one identity, optionally restricted to a
                    single application. Returns assignments only; never credentials,
                    secrets or personal data. Access to another employee's records requires
                    authorization and is always audited. Read-only.""")
    public Mono<IdentityAccessResult> getIdentityAccess(
            @McpToolParam(required = true,
                    description = "Domain ID of the person to report on - the Elevance network "
                            + "login identifier, for example AB12345.")
            String domainId,
            @McpToolParam(required = false,
                    description = "Restrict assignments to one application, by application id.")
            String applicationId,
            @McpToolParam(required = false, description = "Zero-based page index.")
            Integer page,
            @McpToolParam(required = false,
                    description = "Results per page. Defaults to 20, maximum 50.")
            Integer size) {

        return executor.execute(
                ToolName.GET_IDENTITY_ACCESS,
                TargetType.IDENTITY,
                () -> {
                    String identity = require(domainId);
                    return ToolArguments.targeting(
                            new AccessArguments(
                                    identity,
                                    Optional.ofNullable(applicationId).filter(value -> !value.isBlank()),
                                    Pagination.of(page, size)),
                            identity);
                },
                (arguments, context, decision) -> accessClient.accessFor(
                        arguments.domainId(),
                        arguments.applicationId(),
                        arguments.pagination(),
                        context.isSelfServiceFor(arguments.domainId())),
                result -> result.assignments().size());
    }

    private static String require(String domainId) {
        if (domainId == null || domainId.isBlank()) {
            throw new InvalidToolArgumentException("domainId", "must not be blank");
        }
        return domainId.strip();
    }

    record AccessArguments(String domainId, Optional<String> applicationId, Pagination pagination) {
    }
}
