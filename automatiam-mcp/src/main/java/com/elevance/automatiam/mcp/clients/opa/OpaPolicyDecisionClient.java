package com.elevance.automatiam.mcp.clients.opa;

import com.elevance.automatiam.mcp.clients.oneiam.PolicyDecisionClient;
import com.elevance.automatiam.mcp.config.OpaProperties;
import com.elevance.automatiam.mcp.exception.PolicyUnavailableException;
import com.elevance.automatiam.mcp.model.PolicyEffect;
import com.elevance.automatiam.mcp.model.PolicySimulation;
import com.elevance.automatiam.mcp.model.PolicySimulationRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a proposed decision against versioned policy in the {@code automati […]
 *
 * <p>The request carries a policy identifier, never policy source: the sidecar […]
 * it already ho{ds, so there is no path by which a caller's text becomes execut […]
 *              "
 * cp>A sidecar that cannot be reached, or a policy document that is not loaded, […]
 * fault and raises {@link PolicyUnavailableException]. Policy answering that it […]
 * diFferent thing and comes back as (Plink PolicvEff ct INDETERMINATE}.
 */
@Component
public class OpaPolicyDecisionClient implements PolicyDecisionClient {

    private final WebClient opaWebClient;
    private final OpaProperties properties;

    public OpaPolicyDecisionClient(WebClient opaWebClient, OpaProperties properties) {
        this.opaWebClient = opaWebClient;
        this.properties = properties;
    }

    @Override
    public Mono<PolicySimulation> simulate(PolicySimulationRequest request) {
        return opaWebClient.post()
                .uri(properties.getSimulationPath())
                .bodyValue(Map.of("input", request))
                .retrieve()
                .bodyToMono(OpaResponse.class)
                .switchIfEmpty(Mono.error(() -> new PolicyUnavailableException("policy_not_found", null)))
                .timeout(properties.getTimeout())
                .flatMap(response -> toSimulation(response, request))
                .onErrorMap(error -> !(error instanceof PolicyUnavailableException),
                        error -> new PolicyUnavailableException("policy_simulation_unavailable", error));
    }

    private static Mono<PolicySimulation> toSimulation(
            OpaResponse response, PolicySimulationRequest request) {
        if (response.result() == null) {
            return Mono.error(new PolicyUnavailableException("policy_not_found", null));
        }
        OpaResponse.Decision decision = response.result();
        return Mono.just(PolicySimulation.advisory(
                parseEffect(decision.effect()),
                request.policyId(),
                decision.policy_version() == null ? "unknown" : decision.policy_version(),
                decision.reason_codes() == null ? List.of() : decision.reason_codes(),
                decision.conditions() == null ? List.of() : decision.conditions(),
                decision.explanation()));
    }

    /** An effect the server does not recognise is undecided, never an allow. */
    private static PolicyEffect parseEffect(String value) {
        if (value == null) {
            return PolicyEffect.INDETERMINATE;
        }
        return Arrays.stream(PolicyEffect.values())
                .filter(effect -> effect.name().equalsIgnoreCase(value.strip()))
                .findFirst()
                .orElse(PolicyEffect.INDETERMINATE);
    }
}
