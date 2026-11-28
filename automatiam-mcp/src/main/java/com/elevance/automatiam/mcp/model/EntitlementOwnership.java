package com.elevance.automatiam.mcp.model;

import java.util.Optional;

/**
 * The three ownership roles EM records against an entitlement.
 *
 * <p>Distinct from {@link Ownership}, which models a single owner: an entitleme […]
 * application owner, an entitlement owner and a secondary approver, and collaps […]
 * lose the distinction an approver actually needs. Each is a domain ID plus the […]
 * holds alongside it.
 *
 * <p>Returned only when policy authorized ownership disclosure; absent rather t […]
 * otherwise, so a consumer cannot read redaction as "unowned".
 */
public record EntitlementOwnership(
        Optional<Ownership> applicationOwner,
        Optional<Ownership> entitlementOwner,
        Optional<Ownership> secondaryApprover) {

    public EntitlementOwnership {
        applicationOwner = applicationOwner == null ? Optional.empty() : applicationOwner;
        entitlementOwner = entitlementOwner == null ? Optional.empty() : entitlementOwner;
        secondaryApprover = secondaryApprover == null ? Optional.empty() : secondaryApprover;
    }

    public static EntitlementOwnership none() {
        return new EntitlementOwnership(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
