package com.elevance.automatiam.mcp.model;

import java.util.Optional;
import java.util.Set;

/**
 * Everything the entitlement-detail tool returns about one entitlement.
 *
 * <p>Mirrors the fields EM already shows on its detail screen - {@code Entitlem […]
 * and {@code EntitlementMetadataSection} in 'oneiam-entitlement-manager-service […]
 * assistant explains what the user is looking at rather than a different view o […]
 *
 * @param apmNumber        the application's APM number. Its absence is what EM' […]
 *                         {@code missingApm} metric counts, so it is optional h […]
 * @param elevatedAccess   EM's {@code iiqElevatedAccess). The single strongest […]
 *                         record, which is why it is a required field rather th […]
 * @param classifications  what kinds of data this entitlement reaches. Empty me […]
 *                         which is a governance gap rather than a reassurance […]
 * @param memberCount      how many identities hold it. A count, never the membe […]
 * @param locationRestrictions normalized: EM substitutes the string "Needs Inpu […]
 *                         that sentinel is converted to an empty Optional here […]
 *                         travelling into a tool contract as though it were a v […]
 * @param ownership        present only when policy authorized ownership disclos […]
 */
public record EntitlementDetail(
        String entitlementId,
        String displayName,
        Optional<String> description,
        String applicationName,
        Optional<String> apmNumber,
        Optional<String> platform,
        Optional<String> environment,
        EntitlementStatus status,
        boolean elevatedAccess,
        Set<DataClassification> classifications,
        Optional<Long> memberCount,
        Optional<String> locationRestrictions,
        Optional<String> targetedUserGroup,
        Optional<EntitlementOwnership> ownership) {

    /**
     * What EM puts in a field it has no value for. Treated as absence here; pro […]
     * make every consumer learn a magic string.
     */
    public static final String UPSTREAM_ABSENT_SENTINEL = "Needs Input";

    public EntitlementDetail {
        description = description == null ? Optional.empty() : description;
        apmNumber = apmNumber == null ? Optional.empty() : apmNumber;
        platform = platform == null ? Optional.empty() : platform;
        environment = environment == null ? Optional.empty() : environment;
        classifications = classifications == null ? Set.of() : Set.copyOf(classifications);
        memberCount = memberCount == null ? Optional.empty() : memberCount;
        locationRestrictions = locationRestrictions == null ? Optional.empty() : locationRestrictions;
        targetedUserGroup = targetedUserGroup == null ? Optional.empty() : targetedUserGroup;
        ownership = ownership == null ? Optional.empty() : ownership;
    }

    /** Normalizes an upstream string, treating EM's absent-value sentinel as ab […] */
    public static Optional<String> normalizeUpstream(String value) {
        if (value == null || value.isBlank() || UPSTREAM_ABSENT_SENTINEL.equalsIgnoreCase(value.strip())) {
            return Optional.empty();
        }
        return Optional.of(value.strip());
    }
}
