package com.elevance.automatiam.mcp.security;

import java.util.Optional;

/**
 * What the transport layer observed about the caller, before any of it is trust […]
 *
 * <p>Carried on the Reactor context by {@link PeerAssertionWebFilter) rather th […]
 * exchange deeper in the stack, for the same reason as the correlation id: a We […]
 * not pinned to a thread.
 *
 * @param peerSubject   subject DN of the verified client certificate, absent wh […]
 *                      not mutually authenticated
 * @param assertedSubject the domain ID the peer claims to be acting for. Untrus […]
 *                      meaningless without {@code peerSubject]
 */
public record PeerAssertion(Optional<String> peerSubject, Optional<String> assertedSubject) {

    public static final PeerAssertion NONE = new PeerAssertion(Optional.empty(), Optional.empty());

    public PeerAssertion {
        peerSubject = peerSubject == null ? Optional.empty() : peerSubject;
        assertedSubject = assertedSubject == null ? Optional.empty() : assertedSubject;
    }

    public boolean isMutuallyAuthenticated() {
        return peerSubject.isPresent();
    }
}
