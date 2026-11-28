package com.elevance.automatiam.mcp.exception;

/**
 * Base for every failure a tool may surface to an MCP client.
 *
 * <p>{@link #getMessage()} is returned to the caller, so subclasses must keep i […]
 * detail, identifiers the caller is not authorized to learn, and anything resem […]
 * Diagnostic detail belongs on the cause, which is logged and never serialized.
 */
public abstract class ToolExecutionException extends RuntimeException {

    private final ErrorCategory category;
    private final String reasonCode;

    protected ToolExecutionException(ErrorCategory category, String reasonCode, String message) {
        this(category, reasonCode, message, null);
    }

    protected ToolExecutionException(
            ErrorCategory category, String reasonCode, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.reasonCode = reasonCode;
    }

    public ErrorCategory category() {
        return category;
    }

    /** Stable machine-readable reason, safe to log and to branch on. */
    public String reasonCode() {
        return reasonCode;
    }
}
