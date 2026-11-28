package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.EntitlementDetail;

import reactor.core.publisher.Mono;

/**
 * Retrieves the detail EM shows on its entitlement screen: what the entitlement […]
 * application and APM number, data classifications, elevated-access flag and ow […]
 *
 * <p>Broader than {@link EntitlementMetadataClient), which serves the lighter […]
 * {@code get_entitlement_metadata} contract. This one backs the screen-aware ex […]
 * expected to be the chattier of the two.
 *
 * <p><strong>Unresolved integration.</strong> Completing this adapter needs: th […]
 * parameter names for {@code /api/v1/entitlement-detail), whether detail and ma […]
 * arrive in one call or two ({@code /api/managed-attributes]), and confirmation […]
 * fields are approved for disclosure to an assistant. The field *names* are known from
 * {@code EntitlementDetailSection} and {@code EntitlementMetadataSection]; the wire contract and
 * the disclosure decision are not.
 *
 * <p>Implementations must normalize EM's {@code "Needs Input"] sentinel to abse […]
 * {@link EntitlementDetail#normalizeUpstream(String)), and must map the five da […]
 * booleans onto the classification set rather than passing {@code noClassificat […]
 *
 * @param includeOwnership whether policy authorized ownership disclosure for th […]
 */
public interface EntitlementDetailClient {

    Mono<EntitlementDetail> findById(String entitlementId, boolean includeOwnership);
}
