package com.elevance.automatiam.mcp.exception;

/**
 * The server is already running its maximum number of concurrent tool invocatio […]
 *
 * <p>Shed rather than queued. An unbounded queue in front of a fixed upstream t […]
 * into a pile of calls that all time out, and the caller learns nothing until t […]
 */
public class ToolOverloadedException extends ToolExecutionException {

    public ToolOverloadedException(int limit) {
        super(ErrorCategory.RESOURCE_EXHAUSTED, "concurrency_limit_reached",
                "The server is at its concurrent tool invocation limit (" + limit + ").");
    }
}
