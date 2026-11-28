package com.elevance.automatiam.mcp.exception;

/**
 * Error taxonomy carried on audit events and the {@code error_category] metric […]
 *
 * <p>Categories are coarse on purpose. They say enough to alert on and to tell […]
 * of thing went wrong, without leaking which upstream failed or why.
 */
public enum ErrorCategory {

    /** Arguments failed validation before anything was invoked. */
    INVALID_ARGUMENT,

    /** No trusted execution context could be established for the caller. */
    UNAUTHENTICATED,

    /** Policy returned an explicit deny. */
    FORBIDDEN,

    /** The policy decision point could not be reached or returned an unusable a […] */
    POLICY_UNAVAILABLE,

    /** An upstream OneIAM capability failed or was unreachable. */
    UPSTREAM_FAILURE,

    /** An upstream call exceeded its deadline. */
    TIMEOUT,

    /** The server is at its concurrent-invocation limit and shed this request. […] */
    RESOURCE_EXHAUSTED,

    /** The requested resource does not exist, or the caller may not learn that […] */
    NOT_FOUND,

    /** Anything not covered above. Never includes the underlying detail in what […] */
    INTERNAL
}
