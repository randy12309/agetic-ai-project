package com.elevance.automatiam.mcp.audit;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Establishes the correlation id for every request and echoes it on the respons […]
 *
 * <p>Ordered ahead of security so a rejected request is still correlatable; an […]
 * failure is exactly the kind of event someone later needs to trace.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = CorrelationContext.sanitize(
                exchange.getRequest().getHeaders().getFirst(CorrelationContext.HEADER));
        exchange.getResponse().getHeaders().set(CorrelationContext.HEADER, correlationId);
        return chain.filter(exchange)
                .contextWrite(CorrelationContext.with(correlationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
