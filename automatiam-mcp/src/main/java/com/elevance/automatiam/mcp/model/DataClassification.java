package com.elevance.automatiam.mcp.model;

/**
 * Data protection indicators carried by an entitlement, from EM's
 * {@code DataProtectionIndicatorsDto).
 *
 * <p>These describe what kind of data the entitlement grants access to - they a […]
 * the entitlement, not personal data about anybody. That is why they are safe t […]
 * they matter: an approver deciding on {@code claims.adjuster) needs to know it […]
 *
 * <p>Absence is modelled by an empty set, never by a {@code noClassification] m […]
 * saying "no classification" alongside five booleans saying which classificatio […]
 * representations of the same fact, and they can disagree.
 */
public enum DataClassification {

    /** Protected Health Information. */
    PHI,

    /** Protected Financial Information. */
    PFI,

    /** Personal Information. */
    PI,

    /** Personally Identifiable Information. */
    PII,

    /** Company Proprietary Information. */
    CPI
}
