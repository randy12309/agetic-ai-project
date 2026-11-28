package com.elevance.automatiam.mcp.security;

/**
 * The end user a workload is acting for, identified by domain ID and taken from […]
 * token.
 *
 * <p>There is deliberately no constructor path from a tool argument or a reques […]
 * workflow cannot assert who it is acting for; the token says so, or the server […]
 *
 * <p>In particular this server does <strong>not</strong> honour {@code X-Impers […]
 * which the Boot 3 estate's {@code DomainIdResolver) accepts. There, the header […]
 * caller the audit filter has already validated. Here the caller is an LLM-driv […]
 * identity header is exactly the untrusted input the security model excludes. I […]
 * is ever needed, has to arrive as a signed delegation claim.
 */
public record SubjectContext(DomainId domainId) {

    public SubjectContext {
        if (domainId == null) {
            throw new IllegalArgumentException("domainId must not be null");
        }
    }

    public static SubjectContext of(String domainId) {
        return new SubjectContext(new DomainId(domainId));
    }
}
