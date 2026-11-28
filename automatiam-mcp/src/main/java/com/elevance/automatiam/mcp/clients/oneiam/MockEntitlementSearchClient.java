package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.EntitlementMetadata;
import com.elevance.automatiam.mcp.model.EntitlementSearchCriteria;
import com.elevance.automatiam.mcp.model.EntitlementSearchResult;
import com.elevance.automatiam.mcp.model.EntitlementSummary;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
public class MockEntitlementSearchClient implements EntitlementSearchClient {

    @Override
    public Mono<EntitlementSearchResult> search(EntitlementSearchCriteria criteria) {
        return Mono.fromSupplier(() -> {
            String needle = criteria.searchText().toLowerCase(Locale.ROOT);
            List<EntitlementSummary> matches = SyntheticIamData.ENTITLEMENTS.stream()
                    .filter(entitlement -> matchesText(entitlement, needle))
                    .filter(entitlement -> criteria.applicationId()
                            .map(id -> id.equals(entitlement.applicationId())).orElse(true))
                    .filter(entitlement -> criteria.status()
                            .map(status -> status == entitlement.status()).orElse(true))
                    .map(SyntheticIamData::toSummary)
                    .toList();

            List<EntitlementSummary> slice = MockPaging.slice(matches, criteria.pagination());
            return new EntitlementSearchResult(
                    slice, MockPaging.metadata(matches, slice, criteria.pagination()));
        });
    }

    private static boolean matchesText(EntitlementMetadata entitlement, String needle) {
        return entitlement.entitlementName().toLowerCase(Locale.ROOT).contains(needle)
                || entitlement.description()
                        .map(text -> text.toLowerCase(Locale.ROOT).contains(needle))
                        .orElse(false);
    }
}
