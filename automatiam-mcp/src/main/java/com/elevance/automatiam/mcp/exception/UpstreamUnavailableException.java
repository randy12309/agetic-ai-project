package com.elevance.automatiam.mcp.exception;

/**
 * An upstream OneIAM capability failed. The failing system is named in the caus […]
 * event, never in the message handed back to the caller.
 */
public class UpstreamUnavailableException extends ToolExecutionException {

    public UpstreamUnavailableException(ErrorCategory category, String reasonCode, Throwable cause) {
        super(category, reasonCode, "An upstream IAM capability is unavailable.", cause);
    }
}
