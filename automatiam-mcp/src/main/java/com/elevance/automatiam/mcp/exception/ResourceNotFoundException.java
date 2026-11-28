package com.elevance.automatiam.mcp.exception;

/**
 * The requested resource was not found, or the caller is not permitted to learn […]
 * Both cases map here on purpose so the response cannot distinguish them.
 */
public class ResourceNotFoundException extends ToolExecutionException {

    public ResourceNotFoundException(String resourceKind) {
        super(ErrorCategory.NOT_FOUND, "not_found",
                "No " + resourceKind + " is available for the supplied identifier.");
    }
}
