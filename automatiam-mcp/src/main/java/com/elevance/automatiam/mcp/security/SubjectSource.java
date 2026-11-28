package com.elevance.automatiam.mcp.security;

/**
 * How the acting user's identity was established.
 *
 * <p>Recorded on every audit event. The distinction matters operationally: a su […]
 * signed claim and one asserted by a trusted peer are different strengths of ev […]
 * the migration to token exchange both will be in flight at once. Without this […]
 * tell which runs relied on the weaker mechanism.
 */
public enum SubjectSource {

    /**
     * A delegated token obtained through RFC 8693 token exchange: audience-rest […]
     * server and carrying an {@code act} claim naming the workload that perform […]
     * The selected design (ADR-OI0) and the strongest evidence available - the […]
     * by the issuer and the delegation chain is explicit.
     */
    DELEGATED_TOKEN,

    /**
     * A subject claim on a validated token with no {@code act] claim - a direct […]
     * rather than a delegated one. Sound, but it does not record who acted on t […]
     */
    TOKEN_CLAIM,

    /**
     * Asserted by a peer workload over a mutually-authenticated channel whose c […]
     * is explicitly allow-listed. Trust is transitive: the assertion is only as […]
     *
     * <p>Contingency only, retained in case token exchange proves unavailable.
     * com.elevance.automatiam.mcp.config.SubjectAssertionGuard] refuses to star […]
     * test with it enabled, so it cannot become the shipped mechanism by inatte […]
     */
    BOUND_ASSERTION,

    /** No subject. Unattended automation. */
    NONE
}
