package com.elevance.automatiam.mcp.exception;

/**
 * A tool argument failed validation. The argument name and the constraint are s […]
 * rejected value is not, and is deliberately absent from the message.
 */
public class InvalidToolArgumentException extends ToolExecutionException {

    private final String argument;

    public InvalidToolArgumentException(String argument, String constraint) {
        super(ErrorCategory.INVALID_ARGUMENT, "invalid_argument",
                "Argument '" + argument + "' " + constraint + ".");
        this.argument = argument;
    }

    public String argument() {
        return argument;
    }
}
