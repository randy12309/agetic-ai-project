package com.elevance.automatiam.mcp.model;

import java.util.Map;
import java.util.Optional;

/**
 * Full metadata for a single entitlement.
 *
 * @param ownership  present only when policy authorized ownership disclosure
 * @param attributes approved classification attributes; the adapter filters thi […]
 *                   key set before it reaches a caller
 */
public record EntitlementMetadata(
        String entitlementId,
        String entitlementName,
        Optional<String> description,
        String applicationId,
        String applicationName,
        EntitlementStatus status,
        Optional<Ownership> ownership,
        Map<String, String> attributes) {

    public EntitlementMetadata {
        description = description == null ? Optional.empty() : description;
        ownership = ownership == null ? Optional.empty() : ownership;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
