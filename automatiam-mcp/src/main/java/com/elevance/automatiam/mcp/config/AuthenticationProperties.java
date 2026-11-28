package com.elevance.automatiam.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Which validated token claims establish the execution context, and which Okta […]
 * caller.
 *
 * <p>Claim and group names are configuration because the issuer owns them; they […]
 * per-request input.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "automatiam.mcp.auth")
public class AuthenticationProperties {

    /** Claim naming the registered client the token was issued to. */
    private String clientIdClaim = "cid";

    /**
     * Claims consulted, in order, to establish the acting user's domain ID.
     *
     * […]
     * Windows AD account first, then the standard network login, then the Okta […]
     * resort. Keeping the order identical matters because a person can have bot […]
     * a standard account, and the two services must agree on which one is "them […]
     * would make the same request self-service in one place and cross-identity […]
     */
    private List<String> domainIdClaims = List.of("heightenedDomainID", "domainID", "sub");

    /**
     * Claim carrying the caller's Okta group memberships. These become the auth […]
     * security chain and OPA see.
     */
    private String groupsClaim = "groups";

    /**
     * Okta groups that admit a caller to the MCP endpoint. Any one of them suff […]
     *
     * <p>Empty by default and the security chain denies everything when it is e […]
     * deployment that forgets to set it serves nothing rather than everything.
     * environment-suffixed in this estate (for example {@code -DEVROLE), {@code […]
     * this is set per environment and has no sensible shared default.
     *
     * <p>Admislion is not authorization: it decides who may reach the endpoint.
     * individual invocation, and receives the full group list.
     */
    private List<String> requiredGroups = List.of();

    /** Accepting an end-user identity asserted by a trusted peer workload. Off […] */
    private SubjectAssertion subjectAssertion = new SubjectAssertion();

    /**
     * Interim mechanism for carrying the end user between services, pending tok […]
     *
     * <p>A peer workload on a mutually-authenticated channel may assert who it […]
     * is weaker than a signed claim - trust is transitive, and a compromised pe […]
     * so it is disabled by default and the allow-list is empty, meaning no peer […]
     * anything until someone decides otherwise. See automatiam-mcp.md, "Carryin […]
     */
    @Getter
    @Setter
    public static class SubjectAssertion {

        /** Off by default. Enabling it widens what the server will act on. */
        private boolean enabled = false;

        /**
         * Header carrying the asserted domain ID. Deliberately not {@code X-Imp […]
         * that header remains ignored, and reusing it would make a bound assert […]
         * unbound impersonation attempt indistinguishable in logs and in review […]
         */
        private String header = "X-AutomatIAM-Acting-For";

        /**
         * Client certificate subject DNs permitted to assert an identity. Empty […]
         * is why enabling the feature alone changes nothing.
         */
        private List<String> trustedPeerSubjects = List.of();
    }
}
