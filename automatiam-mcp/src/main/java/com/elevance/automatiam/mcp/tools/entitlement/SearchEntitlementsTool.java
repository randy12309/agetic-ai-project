package com.elevance.automatiam.mcp.tools.entitlement;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.EntitlementSearchClient;
import com.elevance.automatiam.mcp.model.EntitlementSearchCriteria;
import com.elevance.automatiam.mcp.model.EntitlementSearchResult;
import com.elevance.automatiam.mcp.model.EntitlementStatus;
import com.elevance.automatiam.mcp.model.EnumParser;
import com.elevance.automatiam.mcp.model.Pagination;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

import java.util.Optional;

public class SearchEntitlementsTool {

    private final ToolExecutor executor;
    private final EntitlementSearchClient searchClient;

    public SearchEntitlementsTool(ToolExecutor executor, EntitlementSearchClient searchClient) {
        this.executor = executor;
        this.searchClient = searchClient;
    }

    @McpTool(
            name = "search_entitlements",
            title = "Search entitlements",
            description = """
                    Search the OneIAM entitlement catalogue by free text, optionally filtered by
                    application or status. Returns only entitlements the caller may see,
                    one page at a time. Read-only: nothing is requested, granted or changed.""")
    public Mono<EntitlementSearchResult> searchEntitlements(
            @McpToolParam(required = true,
                    description = "Text to match against entitlement name and description.")
            String searchText,
            @McpToolParam(required = false,
                    description = "Restrict results to one application, by application id.")
            String applicationId,
            @McpToolParam(required = false,
                    description = "Restrict results by status: ACTIVE, INACTIVE or UNKNOWN.")
            String status,
            @McpToolParam(required = false, description = "Zero-based page index.")
            Integer page,
            @McpToolParam(required = false,
                    description = "Results per page. Defaults to 20, maximum 50.")
            Integer size) {

        return executor.execute(
                ToolName.SEARCH_ENTITLEMENTS,
                TargetType.ENTITLEMENT,
                () -> ToolArguments.untargeted(new EntitlementSearchCriteria(
                        searchText,
                        Optional.ofNullable(applicationId).filter(value -> !value.isBlank()),
                        EnumParser.parseOptional(EntitlementStatus.class, "status", status),
                        Pagination.of(page, size))),
                (criteria, context, decision) -> searchClient.search(criteria),
                result -> result.entitlements().size());
    }
}
