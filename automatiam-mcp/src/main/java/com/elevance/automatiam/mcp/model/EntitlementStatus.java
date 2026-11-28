package com.elevance.automatiam.mcp.model;

/**
 * Status of a entitlement.
 *
 * <p>Mirrors the estate vocabulary in
 * {@code iam-identitymanagementservice/src/main/java/com/elevance/iam/entitleme […]
 * Do not add values here that no OneIAM service emits.
 *
 * <p>{@link #UNKNOWN} is deliberate, and every status enum in the estate has on […]
 * value this server does not recognise maps here rather than failing the read.
 * appearing in OneIAM should degrade a result, not break it.
 */
public enum EntitlementStatus {
    ACTIVE,
    INACTIVE,
    UNKNOWN
}
