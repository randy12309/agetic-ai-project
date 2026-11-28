package com.elevance.automatiam.mcp.clients.opa;

import com.elevance.automatiam.mcp.config.OpaProperties;
import com.elevance.automatiam.mcp.exception.PolicyUnavailableException;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Queries the OPA sidecar for a tool authorization decision.
 *
 * <p>Fails closed in three distinct ways, all of which stop the invocation: the […]
 * unreachable, the policy document does not exist, or the document exists but c […]
 * {@code allow}. The last two are treated as unavailability rather than denial […]
 * or malformed policy is an operational fault, and reporting it as a deny would […]
 * bundle behind what looks like a routine refusal.
 */
@Component
public class OpaToolPolicyClient implements ToolPolicyClient {

    private final WebClient opaWebClient;
    private final OpaProperties properties;

    public OpaToolPolicyClient(WebClient opaWebClient, OpaProperties properties) {
        this.opaWebClient = opaWebClient;
        this.properties = properties;
    }

    @Override
    public Mono<ToolAuthorizationDecision> authorize(ToolAuthorizationRequest request) {
        return opaWebClient.post()
                .uri(properties.getAuthorizationPath())
                .bodyValue(Map.of("input", request))
                .retrieve()
                .bodyToMono(OpaResponse.class)
                .switchIfEmpty(Mono.error(() -> new PolicyUnavailableException("policy_not_found", null)))
                .timeout(properties.getTimeout())
                .flatMap(OpaToolPolicyClient::toDecision)
                .onErrorMap(error -> !(error instanceof PolicyUnavailableException),
                        error -> new PolicyUnavailableException("policy_decision_unavailable", error));
    }

    private static Mono<ToolAuthorizationDecision> toDecision(OpaResponse response) {
        if (response.result() == null) {
            return Mono.error(new PolicyUnavailableException("policy_not_found", null));
        }
        OpaResponse.Decision decision = response.result();
        if (decision.allow() == null) {
            return Mono.error(new PolicyUnavailableException("policy_decision_malformed", null));
        }
        return Mono.just(new ToolAuthorizationDecision(
                decision.allow(),
                decision.reason_code() == null ? "unspecified" : decision.reason_code(),
                decision.policy_version() == null ? "unknown" : decision.policy_version(),
                decision.conditions()));
    }
}
