package com.elevance.automatiam.mcp.tools;

import com.elevance.automatiam.mcp.config.ToolProperties;
import com.elevance.automatiam.mcp.exception.ToolOverloadedException;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.concurrent.Semaphore;

/**
 * Caps concurrent tool invocations.
 *
 * <p>Permits are acquired without blocking and the request is rejected when non […]
 * burst is shed at the edge instead of being queued onto a fixed-size upstream […]
 */
@Component
public class ToolConcurrencyLimiter {

    private final Semaphore permits;
    private final int limit;

    public ToolConcurrencyLimiter(ToolProperties properties) {
        this.limit = properties.getMaxConcurrentInvocations();
        this.permits = new Semaphore(this.limit);
    }

    /**
     * Runs {@code work} while holding a permit, releasing it however the work f […]
     * when the subscriber cancels, which is the case a plain doFinally would mi […]
     */
    public <T> Mono<T> withPermit(Mono<T> work) {
        return Mono.usingWhen(
                acquire(),
                permit -> work,
                permit -> release(),
                (permit, error) -> release(),
                permit -> release());
    }

    int availablePermits() {
        return permits.availablePermits();
    }

    private Mono<Boolean> acquire() {
        return Mono.fromCallable(() -> {
            if (!permits.tryAcquire()) {
                throw new ToolOverloadedException(limit);
            }
            return Boolean.TRUE;
        });
    }

    private Mono<Void> release() {
        return Mono.fromRunnable(permits::release);
    }
}
