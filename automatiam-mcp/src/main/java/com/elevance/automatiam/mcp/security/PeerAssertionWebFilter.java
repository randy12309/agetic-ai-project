package com.elevance.automatiam.mcp.security;

import com.elevance.automatiam.mcp.config.AuthenticationProperties;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Records what the transport actually proved about the caller.
 *
 * <p>The verified client certificate is read from the TLS session, not from a h […]
 * cannot describe its own certificate. The asserted subject <em>is</em> a heade […]
 * here only as a claim - {@link ExecutionContextProvider) decides whether the c […]
 * right to make it.      ,
 *                        g
 * cp><strong>Deployment assumption.</strong> This reads {@code SslInfo} from th […]
 * populated when TLS terminates at this application. If a deployment terminates […]
 * or load balancer instead, the peer certificate arrives as a proxy header and […]
 * replaced - not supplemented - by one that reads it, with the proxy stripping […]
 * copy. Getting that wrong turns the allow-list into a header a caller can set, […]
 * is stated here rather than left to be discovered.
 */
@Component
public class PeerAssertionWebFilter implements WebFilter, Ordered {

    private final AuthenticationProperties properties;

    public PeerAssertionWebFilter(AuthenticationProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        PeerAssertion assertion = new PeerAssertion(
                verifiedPeerSubject(exchange),
                assertedSubject(exchange));

        return chain.filter(exchange).contextWrite(PeerAssertionContext.with(assertion));
    }

    private Optional<String> verifiedPeerSubject(ServerWebExchange exchange) {
        SslInfo sslInfo = exchange.getRequest().getSslInfo();
        if (sslInfo == null) {
            return Optional.empty();
        }
        X509Certificate[] certificates = sslInfo.getPeerCertificates();
        if (certificates == null || certificates.length == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(certificates[0].getSubjectX500Principal())
                .map(principal -> principal.getName())
                .filter(name -> !name.isBlank());
    }

    private Optional<String> assertedSubject(ServerWebExchange exchange) {
        String header = properties.getSubjectAssertion().getHeader();
        return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(header))
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
