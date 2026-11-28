package com.elevance.automatiam.mcp.security;

import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;

/**
 * An Elevance Health domain ID - the network login identifier OneIAM uses to na […]
 *
 * <p>This is the identifier type OneIAM's identity capabilities expect. It is n […]
 * internal subject, and the two are not interchangeable: {@code sub) is only th […]
 * fallback in the estate's resolution order (see
 * {@code oneiam-shared-models/src/main/java/com/elevance/iam/service/identity/D […]
 *
 * <p>The original spelling is preserved for anything sent upstream, while {@lin […]
 * case-insensitively. Domain IDs originate as Windows AD account names, which a […]
 * so a caller that lower-cases one must not thereby look like a different perso […]
 * a self-service read into an apparent cross-identity read, or worse, the rever […]
 */
public record DomainId(String value) {

    private static final int MAX_LENGTH = 64;

    public DomainId {
        if (value == null || value.isBlank()) {
            throw new InvalidToolArgumentException("domainId", "must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidToolArgumentException(
                    "domainId", "must not exceed " + MAX_LENGTH + " characters");
        }
    }

    public boolean matches(String other) {
        return other != null && value.equalsIgnoreCase(other.strip());
    }

    @Override
    public String toString() {
        return value;
    }
}
