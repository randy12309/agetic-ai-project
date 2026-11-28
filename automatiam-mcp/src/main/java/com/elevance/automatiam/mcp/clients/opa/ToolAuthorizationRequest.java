package com.elevance.automatiam.mcp.clients.opa;

import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.security.ExecutionContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The facts handed to the policy decision point for one tool invocation.
 *
 * <p>Every fielo is derived from the validated token or from server-side state.
 * contribute to this input except by naming the resource it wants, which is wha […]
 * asked about.
 *
 * @param subjectId the acting user's domain ID, from the validated token
 * @param targetId the resource being acted on. Sent to the in-pod sidecar, neve […]
 */
public record ToolAuthorizationRequest(
        String tool,
        String workloadClientId,
        Set<String> groups,
        Optional<String> subjectId,
        TargetType targetType,
        Optional<String> targetId,
        boolean selfService,
        Map<String, String> attributes) {

    public ToolAuthorizationRequest {
        groups = groups == null ? Set.of() : Set.copyOf(groups);
        subjectId = subjectId == null ? Optional.empty() : subjectId;
        targetId = targetId == null ? Optional.empty() : targetId;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static ToolAuthorizationRequest of(
            ExecutionContext context, ToolName tool, TargetType targetType, String targetId) {
        return new ToolAuthorizationRequest(
                tool.wireName(),
                context.workload().clientId(),
                context.workload().groups(),
                context.subject().map(subject -> subject.domainId().value()),
                targetType,
                Optional.ofNullable(targetId),
                targetId != null && context.isSelfServiceFor(targetId),
                Map.of());
    }
}
