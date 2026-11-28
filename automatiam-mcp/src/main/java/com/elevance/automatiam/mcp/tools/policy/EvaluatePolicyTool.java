package com.elevance.automatiam.mcp.tools.policy;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.clients.oneiam.PolicyDecisionClient;
import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;
import com.elevance.automatiam.mcp.model.PolicySimulation;
import com.elevance.automatiam.mcp.model.PolicySimulationRequest;
import com.elevance.automatiam.mcp.model.TargetResourceType;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.tools.ToolArguments;
import com.elevance.automatiam.mcp.tools.ToolExecutor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Advisory policy simulation.
 *
 * cp>Answers "what would policy decide", and never performs the action it is as […]
 * things are deliberately impossible through this surface: supplying Rego, sinc […]
 * policy identifier is accepted; asserting a role or permission, since the subj […]
 * validated token and attributes are filtered to an approved key set; and obtai […]
 * authorization, since the result is marked advisory and grants nothing.
 */
@Component
public class EvaluatePolicyTool {

    /**
     * Attribute keys a caller may contribute to an evaluation. Anything else is […]
     * than rejected, so an over-eager caller is not blocked, but cannot widen t […]
     */
    private static final Set<String> APPROVED_ATTRIBUTE_KEYS =
            Set.of("justification", "requestedDuration", "changeTicket");

    private static final int MAX_ATTRIBUTE_LENGTH = 256;

    private final ToolExecutor executor;
    private final PolicyDecisionClient policyDecisionClient;

    public EvaluatePolicyTool(ToolExecutor executor, PolicyDecisionClient policyDecisionClient) {
        this.executor = executor;
        this.policyDecisionClient = policyDecisionClient;
    }

    @McpTool(
            name = "evaluate_policy",
            title = "Evaluate a proposed IAM decision",
            description = """
                    Ask what an approved IAM policy would decide about a proposed action, for example
                    whether an employee would be eligible to own an application. Returns an advisory
                    decision with reason codes and any conditions. This is a simulation: it grants
                    nothing and performs nothing.""")
    public Mono<PolicySimulation> evaluatePolicy(
            @McpToolParam(required = true,
                    description = "An approved policy identifier, for example application.ownership. "
                            + "Policy source code is not accepted.")
            String policyId,
            @McpToolParam(required = true,
                    description = "The proposed action, for example become_owner.")
            String action,
            @McpToolParam(required = true,
                    description = "Kind of resource the action targets: APPLICATION, ENTITLEMENT or SERVICE_IDENTITY.")
            String resourceType,
            @McpToolParam(required = true,
                    description = "Identifier of the resource the action targets.")
            String resourceId,
            @McpToolParam(required = false,
                    description = "Optional justification for the proposed action.")
            String justification) {

        return executor.execute(
                ToolName.EVALUATE_POLICY,
                TargetType.POLICY,
                () -> {
                    String resource = require("resourceId", resourceId);
                    return ToolArguments.targeting(
                            new SimulationArguments(
                                    require("policyId", policyId),
                                    require("action", action),
                                    TargetResourceType.parse(resourceType),
                                    resource,
                                    approvedAttributes(justification)),
                            resource);
                },
                (arguments, context, decision) -> policyDecisionClient.simulate(
                        new PolicySimulationRequest(
                                arguments.policyId(),
                                arguments.action(),
                                arguments.resourceType(),
                                arguments.resourceId(),
                                context.subject().map(subject -> subject.domainId().value()),
                                arguments.attributes())),
                result -> 1);
    }

    // The remaining helpers are reconstructed from the call sites and the captured attribute limits.
    private static Map<String, String> approvedAttributes(String justification) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (justification != null && !justification.isBlank() && APPROVED_ATTRIBUTE_KEYS.contains("justification")) {
            String value = justification.strip();
            if (value.length() > MAX_ATTRIBUTE_LENGTH) {
                throw new InvalidToolArgumentException("justification", "must not exceed " + MAX_ATTRIBUTE_LENGTH + " characters");
            }
            attributes.put("justification", value);
        }
        return Map.copyOf(attributes);
    }

    private static String require(String argument, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidToolArgumentException(argument, "must not be blank");
        }
        return value.strip();
    }

    record SimulationArguments(String policyId, String action, TargetResourceType resourceType,
            String resourceId, Map<String, String> attributes) {
    }
}
