package com.elevance.automatiam.mcp.exception;

/**
 * The policy decision point could not be reached, or answered in a shape the se […]
 * on. Callers see a distinct failure from a deny because the two mean different […]
 * operationally, but neither results in the tool running.
 */
public class PolicyUnavailableException extends ToolExecutionException {

    public PolicyUnavailableException(String reasonCode, Throwable cause) {
        super(ErrorCategory.POLICY_UNAVAILABLE, reasonCode,
                "Authorization could not be evaluated; the request was not performed.", cause);
    }
}
