package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.ApplicationLookupResult;
import com.elevance.automatiam.mcp.model.ApplicationSummary;
import com.elevance.automatiam.mcp.model.Pagination;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Primary
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "automatiam.mcp.mocks", name = "enabled", havingValue = "true")
public class MockApplicationLookupClient implements ApplicationLookupClient {

    @Override
    public Mono<ApplicationLookupResult> lookup(
            String term, Pagination pagination, boolean includeOwnership) {
        return Mono.fromSupplier(() -> {
            String needle = term.toLowerCase(Locale.ROOT);
            List<ApplicationSummary> matches = SyntheticIamData.APPLICATIONS.stream()
                    .filter(application -> application.applicationId().equalsIgnoreCase(term)
                            || application.applicationName().toLowerCase(Locale.ROOT).contains(needle))
                    .map(application -> withOwnership(application, includeOwnership))
                    .toList();

            List<ApplicationSummary> slice = MockPaging.slice(matches, pagination);
            return new ApplicationLookupResult(slice, MockPaging.metadata(matches, slice, pagination));
        });
    }

    private static ApplicationSummary withOwnership(ApplicationSummary source, boolean include) {
        if (include) {
            return source;
        }
        return new ApplicationSummary(source.applicationId(), source.applicationName(),
                source.status(), Optional.empty(), source.attributes());
    }
}
