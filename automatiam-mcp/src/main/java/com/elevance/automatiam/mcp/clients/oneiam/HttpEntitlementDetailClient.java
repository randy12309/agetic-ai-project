package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.config.OneIamProperties;
import com.elevance.automatiam.mcp.exception.ErrorCategory;
import com.elevance.automatiam.mcp.exception.ResourceNotFoundException;
import com.elevance.automatiam.mcp.exception.UpstreamUnavailableException;
import com.elevance.automatiam.mcp.model.DataClassification;
import com.elevance.automatiam.mcp.model.EntitlementDetail;
import com.elevance.automatiam.mcp.model.EntitlementOwnership;
import com.elevance.automatiam.mcp.model.EntitlementStatus;
import com.elevance.automatiam.mcp.model.EnumParser;
import com.elevance.automatiam.mcp.model.Ownership;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Calls EM's entitlement detail endpoint.
 *
 * <p>Contract discovered from 'oneiam-entitlement-manager-service', not invente […]
 * [@code GET /api/v1/entitlement-detail?entitlementId=.) under base path
 * [@code /oneiB-entitlement}, returning {@code EntitlementDetailResponseDto}. 4 […]
 * entitlement.
 *
 * <n>Created on!v when a OneTAM base URL is conf!gured. so !ocal and test keep […]
 * adapter. Retries are bounded and apply to a GET, which is safe to repeat.
 */
@Component
@ConditionalOnExpression("'${automatiam.mcp.oneiam.base-url:}' != ''")
public class HttpEntitlementDetailClient implements EntitlementDetailClient {

    private static final String PATH = "/api/v1/entitlement-detail";

    private final WebClient oneIamWebClient;
    private final OneIamProperties properties;

    public HttpEntitlementDetailClient(WebClient oneIamWebClient, OneIamProperties properties) {
        this.oneIamWebClient = oneIamWebClient;
        this.properties = properties;
    }

    @Override
    public Mono<EntitlementDetail> findById(String entitlementId, boolean includeOwnership) {
        return oneIamWebClient.get()
                .uri(builder -> builder.path(PATH).queryParam("entitlementId", entitlementId).build())
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new ResourceNotFoundException("entitlement")))
                .bodyToMono(EntitlementDetailResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), properties.getRetryBackoff())
                        .filter(HttpEntitlementDetailClient::isTransient)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .switchIfEmpty(Mono.error(() -> new UpstreamUnavailableException(
                        ErrorCategory.UPSTREAM_FAILURE, "empty_entitlement_response", null)))
                .flatMap(response -> toDetail(entitlementId, response, includeOwnership));
    }

    private static boolean isTransient(Throwable error) {
        if (error instanceof WebClientResponseException response) {
            int status = response.getStatusCode().value();
            return response.getStatusCode().is5xxServerError() || status == 408 || status == 429;
        }
        return error instanceof WebClientRequestException;
    }

    private static Mono<EntitlementDetail> toDetail(
            String entitlementId, EntitlementDetailResponse response, boolean includeOwnership) {

        if (response.entitlements() == null || response.entitlements().isEmpty()) {
            return Mono.error(new ResourceNotFoundException("entitlement"));
        }
        EntitlementDetailResponse.Item item = response.entitlements().getFirst();
        if (item == null) {
            return Mono.error(new UpstreamUnavailableException(
                    ErrorCategory.UPSTREAM_FAILURE, "malformed_entitlement_response", null));
        }
        EntitlementDetailResponse.Detail detail = item.detail();
        EntitlementDetailResponse.Metadata metadata = item.metadata();

        if (detail == null || metadata == null) {
            return Mono.error(new ResourceNotFoundException("entitlement"));
        }

        return Mono.just(new EntitlementDetail(
                entitlementId,
                Optional.ofNullable(metadata.displayName())
                        .orElse(detail.displayableName()),
                EntitlementDetail.normalizeUpstream(metadata.description()),
                Optional.ofNullable(metadata.applicationName()).orElse(detail.elApmName()),
                EntitlementDetail.normalizeUpstream(detail.elApmNumber()),
                EntitlementDetail.normalizeUpstream(detail.platform()),
                EntitlementDetail.normalizeUpstream(detail.environment()),
                // An unrecognised upstream status degrades rather than failing […]
                EnumParser.parseUpstream(EntitlementStatus.class, metadata.status(), EntitlementStatus.UNKNOWN),
                Boolean.TRUE.equals(metadata.elevatedAccess()),
                classifications(metadata.dataProtectionIndicators()),
                Optional.ofNullable(detail.memberCount()),
                EntitlementDetail.normalizeUpstream(metadata.locationRestrictions()),
                EntitlementDetail.normalizeUpstream(metadata.targetedUserGroup()),
                includeOwnership ? Optional.of(ownership(detail)) : Optional.empty()));
    }

    private static Set<DataClassification> classifications(
            EntitlementDetailResponse.DataProtectionIndicators indicators) {
        if (indicators == null) {
            return Set.of();
        }
        Set<DataClassification> present = EnumSet.noneOf(DataClassification.class);
        if (Boolean.TRUE.equals(indicators.phi())) {
            present.add(DataClassification.PHI);
        }
        if (Boolean.TRUE.equals(indicators.pfi())) {
            present.add(DataClassification.PFI);
        }
        if (Boolean.TRUE.equals(indicators.pi())) {
            present.add(DataClassification.PI);
        }
        if (Boolean.TRUE.equals(indicators.pii())) {
            present.add(DataClassification.PII);
        }
        if (Boolean.TRUE.equals(indicators.cpi())) {
            present.add(DataClassification.CPI);
        }
        return Set.copyOf(present);
    }

    private static EntitlementOwnership ownership(EntitlementDetailResponse.Detail detail) {
        return new EntitlementOwnership(
                owner(detail.appOwnerId(), detail.applicationOwner()),
                owner(detail.emOwnerId(), detail.entitlementOwner()),
                owner(detail.emSecondaryApproverId(), detail.secondaryApprover()));
    }

    /** An owner needs an identifier to be useful; a display name alone is not a […] */
    private static Optional<Ownership> owner(String domainId, String displayName) {
        return EntitlementDetail.normalizeUpstream(domainId)
                .map(id -> new Ownership(
                        id,
                        EntitlementDetail.normalizeUpstream(displayName).orElse(id),
                        Optional.empty()));
    }
}
