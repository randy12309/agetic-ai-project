package com.elevance.automatiam.mcp.tools.application;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.ApplicationLookupClient;
import com.elevance.automatiam.mcp.clients.opa.PolicyCondition;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.ApplicationLookupResult;
import com.elevance.automatiam.mcp.model.Pagination;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

public class LookupApplicationTool {

    private static final int MIN_TERM_LENGTH = 2;

    private final ToolExecutor executor;
    private final ApplicationLookupClient lookupClient;

    public LookupApplicationTool(ToolExecutor executor, ApplicationLookupClient lookupClient) {
        this.executor = executor;
        this.lookupClient = lookupClient;
    }

    @McpTool(
            name = "lookup_application",
            title = "Look up an application",
            description = """
                    Find a OneIAM application by identifier or by name fragment. Returns the application
                    name, status, and ownership and approved attributes where the caller is authorized
                    to see them. Read-only.""")
    public Mono<ApplicationLookupResult> lookupApplication(
            @McpToolParam(required = true,
                    description = "An application id (for example APP-1001) or part of its name.")
            String applicationIdOrTerm,
            @McpToolParam(required = false, description = "Zero-based page index.")
            Integer page,
            @McpToolParam(required = false,
                    description = "Results per page. Defaults to 20, maximum 50.")
            Integer size) {

        return executor.execute(
                ToolName.LOOKUP_APPLICATION,
                TargetType.APPLICATION,
                () -> {
                    String term = require(applicationIdOrTerm);
                    return ToolArguments.targeting(
                            new LookupArguments(term, Pagination.of(page, size)), term);
                },
                (arguments, context, decision) -> lookupClient.lookup(
                        arguments.term(), arguments.pagination(),
                        PolicyCondition.ownershipPermitted(decision)),
                result -> result.applications().size());
    }

    private static String require(String term) {
        if (term == null || term.isBlank()) {
            throw new InvalidToolArgumentException("applicationIdOrTerm", "must not be blank");
        }
        String stripped = term.strip();
        if (stripped.length() < MIN_TERM_LENGTH) {
            throw new InvalidToolArgumentException(
                    "applicationIdOrTerm", "must be at least " + MIN_TERM_LENGTH + " characters");
        }
        return stripped;
    }

    record LookupArguments(String term, Pagination pagination) {
    }
}
