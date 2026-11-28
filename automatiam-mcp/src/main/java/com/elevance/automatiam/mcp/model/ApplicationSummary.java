package com.elevance.automatiam.mcp.model;

import java.util.Map;
import java.util.Optional;

public record ApplicationSummary(
        String applicationId,
        String applicationName,
        ApplicationStatus status,
        Optional<Ownership> ownership,
        Map<String, String> attributes) {

    public ApplicationSummary {
        ownership = ownership == null ? Optional.empty() : ownership;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
