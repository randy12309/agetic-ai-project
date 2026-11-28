package com.elevance.automatiam.mcp.exception;

import com.elevance.automatiam.mcp.model.ToolName;

/**
 * Policy denied the invocation. The reason code comes from OPA and is intended […]
 * the message never states what the target is or whether it exists, so a denial […]
 * probe for the existence of an identity or entitlement.
 */
public class ToolAuthorizationException extends ToolExecutionException {

    public ToolAuthorizationException(ToolName tool, String policyReasonCode) {
        super(ErrorCategory.FORBIDDEN, policyReasonCode,
                "Not authorized to invoke '" + tool.wireName() + "'.");
    }
}
