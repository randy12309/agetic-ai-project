package com.elevance.automatiam.mcp.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Transport security for the MCP endpoint.
 *
 * Bearer tokens only, validated against the configured issuer. […]
 * […] login and no basic auth. […]
 *
 * Authorities come from the token's Okta group claim. […]
 * The chain ends in {@code denyAll}. […]
 * […]
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            AuthenticationProperties authentication,
            WebEndpointProperties endpoints) {

        String actuatorBase = endpoints.getBasePath();
        String[] admitted = authentication.getRequiredGroups().toArray(String[]::new);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> {
                    exchanges
                            .pathMatchers(actuatorBase + "/health/**", actuatorBase + "/prometheus").permitAll();
                    // No configured group means nobody is admitted. A deploymen […]
                    // set this serves nothing, rather than serving everyone.
                    if (admitted.length == 0) {
                        exchanges.pathMatchers("/mcp/**").denyAll();
                    } else {
                        exchanges.pathMatchers("/mcp/**").hasAnyAuthority(admitted);
                    }
                    exchanges.anyExchange().denyAll();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(groupAuthorities(authentication))))
                .build();
    }

    /**
     * Maps the Okta group claim onto authorities with no prefix, so a policy or […]
     * group exactly as Okta does ({@code ONEIAM-...)) rather than a transformed […]
     */
    private static ReactiveJwtAuthenticationConverter groupAuthorities(
            AuthenticationProperties properties) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(properties.getGroupsClaim());
        authorities.setAuthorityPrefix("");

        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new ReactiveJwtGrantedAuthoritiesConverterAdapter(authorities));
        return converter;
    }
}
