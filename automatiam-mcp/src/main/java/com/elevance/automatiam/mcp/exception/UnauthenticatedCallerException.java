package com.elevance.automatiam.mcp.exception;

/**
 * No trusted execution context could be established. Thrown when the request ca […]
 * workload identity, or carries one the server will not act on.
 */
public class UnauthenticatedCallerException extends ToolExecutionException {

    public UnauthenticatedCallerException(String reasonCode) {
        super(ErrorCategory.UNAUTHENTICATED, reasonCode,
                "No trusted execution context for this request.");
    }
}
