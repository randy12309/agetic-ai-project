package com.elevance.automatiam.mcp.security;

import com.elevance.automatiam.mcp.audit.CorrelationContext;
import com.elevance.automatiam.mcp.config.AuthenticationProperties;
import com.elevance.automatiam.mcp.exception.UnauthenticatedCallerException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the trusted {@link ExecutionContext] for the current request.
 *
 * cp>Fails closed. An absent security context, an authentication that is not a […]
 * a token with no usable client identifier all yield {@link UnauthenticatedCall […]
 * than an anonymous context.
 *
 * <p>The acting user is established from one of two sources, in strict order of […]
 * on the validated token, or - only when explicitly enabled - an assertion from […]
 * over a mutually-authenticated channel whose certificate is allow-listed. Toke […]
 * as token exchange rolls out the assertion path falls out of use without a cod […]
 *
 * <p>A token carrying an RFC B693 {@code act} claim is recognised as a delegate […]
 * subject is signed by the issuer and the workload that performed the exchange […]
 * the design ADR-010 selects, and the {@code act) chain is recorded on every au […]
 */
@Component
public class ExecutionContextProvider {

    private final AuthenticationProperties properties;

    public ExecutionContextProvider(AuthenticationProperties properties) {
        this.properties = properties;
    }

    /**
     * Resolves the execution context for the in-flight request.
     *
     * @return the context, or an error signal if no trusted context can be esta […]
     */
    public Mono<ExecutionContext> current() {
        return Mono.deferContextual(view -> {
            PeerAssertion peer = PeerAssertionContext.from(view);
            String correlationId = CorrelationContext.from(view);

            return ReactiveSecurityContextHolder.getContext()
                    .map(context -> context.getAuthentication())
                    .switchIfEmpty(Mono.error(new UnauthenticatedCallerException("missing_authentication")))
                    .flatMap(authentication -> toExecutionContext(authentication, peer, correlationId));
        });
    }

    private Mono<ExecutionContext> toExecutionContext(
            Authentication authentication, PeerAssertion peer, String correlationId) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return Mono.error(new UnauthenticatedCallerException("unsupported_authentication"));
        }
        if (!jwtAuthentication.isAuthenticated()) {
            return Mono.error(new UnauthenticatedCallerException("authentication_required"));
        }

        Jwt token = jwtAuthentication.getToken();
        Optional<String> clientId = claim(token, properties.getClientIdClaim());
        if (clientId.isEmpty()) {
            return Mono.error(new UnauthenticatedCallerException("missing_client_id"));
        }

        Set<String> groups = jwtAuthentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return resolveSubject(token, peer).map(resolved -> new ExecutionContext(
                new WorkloadIdentity(clientId.get(), groups),
                resolved.subject(),
                resolved.source(),
                delegatingActor(token),
                correlationId,
                UUID.randomUUID().toString()));
    }

    /**
     * Establishes the acting user, or fails.
     *
     * <p>An assertion header that is present is either honoured or the request […]
     * never quietly dropped. Dropping it would answer about the calling workloa […]
     * believes it asked about someone else, which is the same incoherence that […]
     * out of impersonation sessions.
     */
    private Mono<ResolvedSubject> resolveSubject(Jwt token, PeerAssertion peer) {
        Optional<String> fromToken = properties.getDomainIdClaims().stream()
                .map(name -> claim(token, name))
                .flatMap(Optional::stream)
                .findFirst();

        if (fromToken.isPresent()) {
            SubjectSource source = delegatingActor(token).isPresent()
                    ? SubjectSource.DELEGATED_TOKEN
                    : SubjectSource.TOKEN_CLAIM;
            return Mono.just(new ResolvedSubject(Optional.of(SubjectContext.of(fromToken.get())), source));
        }

        if (peer.assertedSubject().isEmpty()) {
            return Mono.just(new ResolvedSubject(Optional.empty(), SubjectSource.NONE));
        }

        AuthenticationProperties.SubjectAssertion settings = properties.getSubjectAssertion();
        if (!settings.isEnabled()) {
            return Mono.error(new UnauthenticatedCallerException("subject_assertion_disabled"));
        }
        if (!peer.isMutuallyAuthenticated()) {
            return Mono.error(new UnauthenticatedCallerException("subject_assertion_requires_mtls"));
        }
        if (!settings.getTrustedPeerSubjects().contains(peer.peerSubject().orElseThrow())) {
            return Mono.error(new UnauthenticatedCallerException("subject_assertion_untrusted_peer"));
        }

        return Mono.just(new ResolvedSubject(
                Optional.of(SubjectContext.of(peer.assertedSubject().orElseThrow())),
                SubjectSource.BOUND_ASSERTION));
    }

    /**
     * The workload named in the RFC 8693 {@code act) claim, which is an object […]
     * {@code sub}. Absent on a direct user token. Only the immediate actor is r […]
     * delegation chain is not flattened, because recording a chain the server d […]
     * would imply a check that is not happening.
     */
    private static Optional<String> delegatingActor(Jwt token) {
        if (!(token.getClaims().get("act") instanceof java.util.Map<?, ?> actor)) {
            return Optional.empty();
        }
        return Optional.ofNullable(actor.get("sub"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(subject -> !subject.isBlank());
    }

    private static Optional<String> claim(Jwt token, String name) {
        Object value = token.getClaims().get(name);
        if (value instanceof String text && !text.isBlank()) {
            return Optional.of(text);
        }
        return Optional.empty();
    }

    private record ResolvedSubject(Optional<SubjectContext> subject, SubjectSource source) {
    }
}
