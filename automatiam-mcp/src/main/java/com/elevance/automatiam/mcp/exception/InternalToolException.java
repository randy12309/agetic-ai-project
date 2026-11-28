package com.elevance.automatiam.mcp.exception;

/**
 * An unexpected failure. The cause is logged; the caller is told only that the […]
 * because anything more specific about an unanticipated error risks describing […]
 */
public class InternalToolException extends ToolExecutionException {

    public InternalToolException(Throwable cause) {
        super(ErrorCategory.INTERNAL, "internal_error", "The tool call could not be completed.", cause);
    }
}
