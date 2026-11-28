package com.elevance.automatiam.mcp.model;

/**
 * Contract version carried on every audit event, so a replayed decision can be […]
 * shape that produced it. Bump when a tool's input or output contract changes.
 */
public final class ToolVersion {

    public static final String CURRENT = "1.0.0";

    private ToolVersion() {
    }
}
