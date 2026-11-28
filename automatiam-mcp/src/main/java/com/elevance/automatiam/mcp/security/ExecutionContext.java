package com.elevance.automatiam.mcp.security;

import java.util.Optional;

/**
 * Everything the server trusts about one tool invocation.
 *
 * <p>Assembled once per request from the validated token and the correlation fi […]
 * down. Nothing on it originates from tool arguments or request headers.
 *
 * @param workload      the authenticated calling workload
 * @param subject       the end user being acted for, absent for unattended auto […]
 * @param subjectSource how that identity was established, recorded for audit
 * @param delegatingActor the workload named in the token's RFC 8693 {@code act] […]
 *                      only on a delegated token. Records who acted for the use […]
 * @param correlationId correlates this call across AutomatIAM services
 * @param requestId     identifies this single invocation
 */
public record ExecutionContext(
        WorkloadIdentity workload,
        Optional<SubjectContext> subject,
        SubjectSource subjectSource,
        Optional<String> delegatingActor,
        String correlationId,
        String requestId) {

    public ExecutionContext {
        subject = subject == null ? Optional.empty() : subject;
        subjectSource = subjectSource == null ? SubjectSource.NONE : subjectSource;
        delegatingActor = delegatingActor == null ? Optional.empty() : delegatingActor;
    }

    /**
     * Whether this call is a caller asking about themselves.
     *
     * <p>Compares the domain ID from the token against the one named in the too […]
     * insensitively. Used to tag audit and to let policy separate self-service […]
     * another employee; it is an input to the decision, never the decision itse […]
     */
    public boolean isSelfServiceFor(String targetDomainId) {
        return subject
                .map(context -> context.domainId().matches(targetDomainId))
                .orElse(false);
    }
}
