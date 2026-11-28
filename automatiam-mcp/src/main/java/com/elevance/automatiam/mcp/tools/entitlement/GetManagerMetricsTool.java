package com.elevance.automatiam.mcp.tools.entitlement;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.ManagerMetricsClient;
import com.elevance.automatiam.mcp.clients.oneiam.ManagerOrganizationClient;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.ManagerScope;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Governance counts for a manager's entitlement scope.
 *
 * cp>The taMget is a person, so this runs against {@link TargetType#IDENTITY} r […]
 * [@code ENT[TLEMENT}: a manager reading their own dashboard is self-service, a […]
 * manager's is a cross-identity query that needs the same authorization as any […]
 * argument means "mine", resolved from the token - the common case, and the one […]
 * used to probe who manages whom.
 *
 * <p>Composed from two services: the organization comes from the identity manag […]
 * entitlement counts from the entitlement manager. Neither is recomputed here - […]
 * from raw identity data would mean holding aggregation that EM owns.
 *
 * <p>If either service fails the whole call fails. A partial answer is worse th […]
 * assistant would report "12 reports, nothing needing attention" when the secon […]
 * unknown, and the caller has no way to tell.
 *
 * <p>Counts only, never rosters. Which entitlements are missing information is […]
 * {@code search_entitlements} with a filter, and who reports to someone is a di […]
 * deliberately does not make - both are authorized separately, so a summary can […]
 * export.
 */
public class GetManagerMetricsTool {

    private final ToolExecutor executor;
    private final ManagerMetricsClient metricsClient;
    private final ManagerOrganizationClient organizationClient;

    public GetManagerMetricsTool(
            ToolExecutor executor,
            ManagerMetricsClient metricsClient,
            ManagerOrganizationClient organizationClient) {
        this.executor = executor;
        this.metricsClient = metricsClient;
        this.organizationClient = organizationClient;
    }

    @McpTool(
            name = "get_manager_metrics",
            title = "Get a manager's governance counts",
            description = """
                    Retrieve a manager's scope: how many people report to them, and governance
                    counts for their entitlements - total, how many are missing
                    information, how many have no APM number, and how many have a pending
                    transfer. Omit the manager to get your own. Returns counts only, never the
                    entitlements and never the people. Read-only.""")
    public Mono<ManagerScope> getManagerMetrics(
            @McpToolParam(required = false,
                    description = "Domain ID of the manager to report on. Omit for your own scope.")
            String managerDomainId) {

        return executor.execute(
                ToolName.GET_MANAGER_METRICS,
                TargetType.IDENTITY,
                () -> {
                    Optional<String> requested = Optional.ofNullable(managerDomainId)
                            .map(String::strip)
                            .filter(value -> !value.isEmpty());
                    return requested.isPresent()
                            ? ToolArguments.targeting(requested, requested.get())
                            : ToolArguments.targetingCaller(requested);
                },
                (requested, context, decision) -> {
                    String target = resolveTarget(requested,
                            context.subject().map(subject -> subject.domainId().value()));
                    return Mono.zip(
                            organizationClient.organizationFor(target),
                            metricsClient.metricsFor(target),
                            ManagerScope::of);
                },
                result -> 1);
    }

    /**
     * An omitted argument means the caller. If the token carries no subject the […]
     * default to, and guessing would mean answering about the wrong person.
     */
    private static String resolveTarget(Optional<String> requested, Optional<String> callerDomainId) {
        return requested.or(() -> callerDomainId)
                .orElseThrow(() -> new InvalidToolArgumentException(
                        "managerDomainId",
                        "must be supplied when the request carries no acting user"));
    }
}
