package com.elevance.automatiam.mcp.clients.opa;

import java.util.List;
import java.util.Map;

/**
 * OPA's {@code /v1/data} envelope. A document that does not exist comes back as […]
 * {@code result}, which is why {@code result) is nullable here and treated as u […]
 * than as a deny.
 */
public record OpaResponse(Decision result) {

    public record Decision(
            Boolean allow,
            String reason_code,
            String policy_version,
            List<String> conditions,
            String effect,
            String explanation,
            List<String> reason_codes,
            Map<String, Object> attributes) {
    }
}
