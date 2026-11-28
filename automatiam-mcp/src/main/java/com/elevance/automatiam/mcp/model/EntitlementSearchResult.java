package com.elevance.automatiam.mcp.model;

import java.util.List;

public record EntitlementSearchResult(List<EntitlementSummary> entitlements, PageMetadata page) {

    public EntitlementSearchResult {
        entitlements = entitlements == null ? List.of() : List.copyOf(entitlements);
    }
}
