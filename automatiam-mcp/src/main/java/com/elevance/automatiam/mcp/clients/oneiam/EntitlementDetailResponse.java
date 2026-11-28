package com.elevance.automatiam.mcp.clients.oneiam;

import java.util.List;

/**
 * The wire shape of {@code GET /api/v1/entitlement-detail?entitlementId=w), mir […]
 * {@code EntitlementDetailResponseDto} and the three sections beneath it.
 *
 * <p>Only the fields this server maps are declared. Unknown properties are igno […]
 * field does not break the adapter - and so a field arriving here does not sile […]
 * what the assistant discloses.
 *
 * <p>Note the single-element list: EM returns {@code entitlements] as an array […]
 * by one id. Treating it as an object would work until the day it holds two.
 */
public record EntitlementDetailResponse(List<Item> entitlements) {

    public record Item(Detail detail, Metadata metadata) {
    }

    public record Detail(
            String displayableName,
            String elApmNumber,
            String elApmName,
            String platform,
            String environment,
            Long memberCount,
            String applicationOwner,
            String entitlementOwner,
            String secondaryApprover,
            String appOwnerId,
            String emOwnerId,
            String emSecondaryApproverId) {
    }

    public record Metadata(
            String applicationName,
            String description,
            String displayName,
            Boolean elevatedAccess,
            String status,
            DataProtectionIndicators dataProtectionIndicators,
            String locationRestrictions,
            String targetedUserGroup) {
    }

    /**
     * EM's five classification booleans plus {@code noClassification]. Mapped t […]
     * {@code noClassification} is deliberately not carried forward, because an […]
     * says it and two representations of one fact can disagree.
     */
    public record DataProtectionIndicators(
            Boolean phi, Boolean pfi, Boolean pi, Boolean pii, Boolean cpi, Boolean noClassification) {
    }
}
