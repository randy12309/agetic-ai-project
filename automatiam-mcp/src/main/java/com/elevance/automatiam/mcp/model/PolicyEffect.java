package com.elevance.automatiam.mcp.model;

/**
 * Outcome of an advisory policy simulation.
 *
 * <p>{@link #INDETERMINATE} is a distinct outcome rather than a mapping onto {@ […]
 * could not decide" and "policy decided no" are different facts, and collapsing […]
 * outage read as a definitive rejection.
 */
public enum PolicyEffect {
    ALLOW,
    DENY,
    INDETERMINATE
}
