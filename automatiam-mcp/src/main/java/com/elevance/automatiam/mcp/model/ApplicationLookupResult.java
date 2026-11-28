package com.elevance.automatiam.mcp.model;

import java.util.List;

public record ApplicationLookupResult(List<ApplicationSummary> applications, PageMetadata page) {

    public ApplicationLookupResult {
        applications = applications == null ? List.of() : List.copyOf(applications);
    }
}
