package com.elevance.automatiam.mcp.config;

import com.elevance.automatiam.mcp.audit.CorrelationContext;

import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Propagates the correlation id onto every outbound call, read from the Rea […]
     * inbound filter established.
     */
    static ExchangeFilterFunction correlationPropagation() {
        return (request, next) -> Mono.deferContextual(view -> next.exchange(
                ClientRequest.from(request)
                        .header(CorrelationContext.HEADER, CorrelationContext.from(view))
                        .build()));
    }

    @Bean
    WebClient opaWebClient(OpaProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .responseTimeout(properties.getTimeout());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Only created once a OneIAM base URL is configured. In local and test the […]
     * stand in, and an unconfigured client that silently resolves nowhere would […]
     * absence.
     */
    @Bean
    // An expression rather than @ConditionalOnProperty: the property is always […]
    // an empty default so the placeholder resolves locally), and @ConditionalOn […]
    // empty as "configured" and build a client pointing nowhere.
    @ConditionalOnExpression("'${automatiam.mcp.oneiam.base-url:}' != ''")
    WebClient oneIamWebClient(OneIamProperties properties) {
        ConnectionProvider connections = ConnectionProvider.builder("oneiam")
                .maxConnections(properties.getMaxConnections())
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .build();

        HttpClient httpClient = HttpClient.create(connections)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getConnectTimeout().toMillis())
                .responseTimeout(properties.getResponseTimeout());

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(correlationPropagation())
                .build();
    }
}
