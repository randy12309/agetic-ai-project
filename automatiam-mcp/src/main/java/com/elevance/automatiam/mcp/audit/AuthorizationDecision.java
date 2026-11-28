package com.elevance.automatiam.mcp.audit;

/**
 * What the policy decision point said, as recorded on the audit event.
 *
 * <p>{@link #NOT_EVALUATED} distinguishes a request that failed before reaching […]
 * policy actively permitted, which a boolean could not express.
 */
public enum AuthorizationDecision {
    PERMIT,
    DENY,
    NOT_EVALUATED
}
