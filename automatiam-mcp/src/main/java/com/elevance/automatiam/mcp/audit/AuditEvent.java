package com.elevance.automatiam.mcp.audit;

import com.elevance.automatiam.mcp.exception.ErrorCategory;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.security.SubjectSource;

import lombok.Builder;

import java.util.Optional;

/**
 * One tool execution, as recorded for the Splunk pipeline.
 *
 * @param resultCount number of elements returned, never the elements themselves […]
 * @param targetType  the kind of resource acted on; the identifier is deliberat […]
 * @param subjectSource how the acting user was established, so runs relying on […]
 *                    interim mechanism can be found later
 */
@Builder
public record AuditEvent(
        String correlationId,
        String requestId,
        String workloadClientId,
        ToolName tool,
        String toolVersion,
        AuthorizationDecision decision,
        Optional<String> policyVersion,
        TargetType targetType,
        boolean subjectPresent,
        SubjectSource subjectSource,
        Optional<String> delegatingActor,
        boolean selfService,
        long durationMillis,
        AuditOutcome outcome,
        Optional<ErrorCategory> errorCategory,
        Optional<String> reasonCode,
        int resultCount) {

    public AuditEvent {
        policyVersion = policyVersion == null ? Optional.empty() : policyVersion;
        subjectSource = subjectSource == null ? SubjectSource.NONE : subjectSource;
        delegatingActor = delegatingActor == null ? Optional.empty() : delegatingActor;
        errorCategory = errorCategory == null ? Optional.empty() : errorCategory;
        reasonCode = reasonCode == null ? Optional.empty() : reasonCode;
    }
}
