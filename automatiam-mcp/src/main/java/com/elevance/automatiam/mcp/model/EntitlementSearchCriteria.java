package com.elevance.automatiam.mcp.model;

import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;

import java.util.Optional;

/**
 * Validated criteria for {@code search_entitlements).
 *
 * <p>The minimum search-text length is an enumeration control: a one-character […]
 * the catalogue and turns search into a bulk listing.
 */
public record EntitlementSearchCriteria(
        String searchText,
        Optional<String> applicationId,
        Optional<EntitlementStatus> status,
        Pagination pagination) {

    public static final int MIN_SEARCH_TEXT_LENGTH = 2;
    public static final int MAX_SEARCH_TEXT_LENGTH = 128;

    public EntitlementSearchCriteria {
        if (searchText == null || searchText.isBlank()) {
            throw new InvalidToolArgumentException("searchText", "must not be blank");
        }
        searchText = searchText.strip();
        if (searchText.length() < MIN_SEARCH_TEXT_LENGTH) {
            throw new InvalidToolArgumentException(
                    "searchText", "must be at least " + MIN_SEARCH_TEXT_LENGTH + " characters");
        }
        if (searchText.length() > MAX_SEARCH_TEXT_LENGTH) {
            throw new InvalidToolArgumentException(
                    "searchText", "must not exceed " + MAX_SEARCH_TEXT_LENGTH + " characters");
        }
        applicationId = applicationId == null ? Optional.empty() : applicationId;
        status = status == null ? Optional.empty() : status;
    }
}
