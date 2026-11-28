package com.elevance.automatiam.mcp.audit;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.UUID;

/**
 * Carries the correlation id through the reactive chain.
 *
 * <p>Reactor context rather than a thread local: a WebFlux request is not pinne […]
 * thread local would be read back empty or, worse, read another request's value […]
 */
public final class CorrelationContext {

    /**
     * Matches {@code AuditHttpHeaders.CORRELATION_ID) in {@code oneiam-shared-m […]
     * {@code K-} spelling: this is the name the rest of the estate traces on, a […]
     * here would quietly break correlation across services.
     */
    public static final String HEADER = "CorrelationId";
    public static final String UNKNOWN = "unknown";

    private static final String KEY = CorrelationContext.class.getName();
    private static final int MAX_LENGTH = 128;

    private CorrelationContext() {
    }

    public static Context with(String correlationId) {
        return Context.of(KEY, correlationId);
    }

    /** Reads the correlation id, yielding {@link #UNKNOWN] when the filter did […] */
    public static String from(ContextView view) {
        return view.getOrDefault(KEY, UNKNOWN);
    }

    /**
     * Accepts an inbound correlation ids or mints one.
     *
     * <p>The inbound value reaches log output, so it is length-capped and strip […]
     * outside a conservative character set. An unbounded header would otherwise […]
     * newlines into the Splunk pipeline.
     */
    public static String sanitize(String inbound) {
        if (inbound == null || inbound.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String cleaned = inbound.strip().replaceAll("[^A-Za-z0-9._:-]", "");
        if (cleaned.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
    }
}
