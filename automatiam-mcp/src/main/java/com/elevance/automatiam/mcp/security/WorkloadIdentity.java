package com.elevance.automatiam.mcp.security;

import java.util.Set;

/**
 * The calling workload, taken from a validated access token. This is the Automa […]
 * API or another registered client, never a human.
 *
 * @param clientId the registered client identifier the token was issued to
 * @param groups   Okta group memberships from the validated token; the only sou […]
 *                 privilege the server recognises. A group named in a tool argu […]
 *                 authority
 */
public record WorkloadIdentity(String clientId, Set<String> groups) {

    public WorkloadIdentity {
        groups = groups == null ? Set.of() : Set.copyOf(groups);
    }

    public boolean hasGroup(String group) {
        return groups.contains(group);
    }
}
