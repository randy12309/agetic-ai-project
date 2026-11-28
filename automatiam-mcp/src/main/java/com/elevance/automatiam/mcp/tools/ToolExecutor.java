package com.elevance.automatiam.mcp.tools;

import com.elevance.automatiam.mcp.audit.AuditEvent;
import com.elevance.automatiam.mcp.audit.AuditOutcome;
import com.elevance.automatiam.mcp.audit.AuthorizationDecision;
import com.elevance.automatiam.mcp.audit.CorrelationContext;
import com.elevance.automatiam.mcp.audit.TargetType;
import com.elevance.automatiam.mcp.audit.ToolAuditLogger;
import com.elevance.automatiam.mcp.audit.ToolMetrics;
import com.elevance.automatiam.mcp.clients.opa.ToolAuthorizationDecision;
import com.elevance.automatiam.mcp.clients.opa.ToolAuthorizationRequest;
import com.elevance.automatiam.mcp.clients.opa.ToolPolicyClient;
import com.elevance.automatiam.mcp.config.ToolProperties;
import com.elevance.automatiam.mcp.exception.ErrorCategory;
import com.elevance.automatiam.mcp.exception.InternalToolException;
import com.elevance.automatiam.mcp.exception.ToolAuthorizationException;
import com.elevance.automatiam.mcp.exception.ToolExecutionException;
import com.elevance.automatiam.mcp.exception.UpstreamUnavailableException;
import com.elevance.automatiam.mcp.model.ToolName;
import com.elevance.automatiam.mcp.model.ToolVersion;
import com.elevance.automatiam.mcp.security.ExecutionContext;
import com.elevance.automatiam.mcp.security.ExecutionContextProvider;
import com.elevance.automatiam.mcp.security.SubjectSource;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * The execution sequence every tool runs: validate, establish a trusted context […]
 * invoke the capability, audit, return.
 *
 * <p>It lives here once rather than in each tool because the parts worth gettin […]
 * closed when policy cannot be reached, auditing a denial as carefully as a suc […]
 * upstream detail out of what the caller sees - are the parts that quietly rot […]
 * times.
 */
@Component
public class ToolExecutor {

    private static final Set<ErrorCategory> UPSTREAM_CATEGORIES = Set.of(
            ErrorCategory.UPSTREAM_FAILURE, ErrorCategory.TIMEOUT, ErrorCategory.NOT_FOUND);

    private final ExecutionContextProvider contextProvider;
    private final ToolPolicyClient policyClient;
    private final ToolAuditLogger auditLogger;
    private final ToolMetrics metrics;
    private final ToolConcurrencyLimiter limiter;
    private final ToolProperties properties;

    public ToolExecutor(
            ExecutionContextProvider contextProvider,
            ToolPolicyClient policyClient,
            ToolAuditLogger auditLogger,
            ToolMetrics metrics,
            ToolConcurrencyLimiter limiter,
            ToolProperties properties) {
        this.contextProvider = contextProvider;
        this.policyClient = policyClient;
        this.auditLogger = auditLogger;
        this.metrics = metrics;
        this.limiter = limiter;
        this.properties = properties;
    }

    /**
     * Runs one tool invocation end to end.
     *
     * <p>{@code arguments) is evaluated inside the pipeline rather than by the […]
     * validation failure is ordered ahead of authentication and is still audite […]
     *
     * @param capability  the OneIAM call. Reached only when policy returned a p […]
     * @param resultCount how many elements the result carries, for the audit ev […]
     *                    itself is never audited
     */
    public <A, T> Mono<T> execute(
            ToolName tool,
            TargetType targetType,
            Supplier<ToolArguments<A>> arguments,
            ToolCapability<A, T> capability,
            ToIntFunction<T> resultCount) {

        return Mono.deferContextual(view -> {
            InvocationTrace trace = new InvocationTrace(CorrelationContext.from(view), tool, targetType);

            Mono<T> pipeline = Mono.fromCallable(arguments::get)
                    .flatMap(validated -> contextProvider.current()
                            .flatMap(context -> authorizeThenInvoke(
                                    tool, targetType, validated, context, capability, trace)))
                    .timeout(properties.getExecutionTimeout());

            return limiter.withPermit(pipeline)
                    .doOnSuccess(result -> auditSuccess(trace, result, resultCount))
                    .onErrorMap(ToolExecutor::normalize)
                    .doOnError(ToolExecutionException.class, error -> auditFailure(trace, error));
        });
    }

    private <A, T> Mono<T> authorizeThenInvoke(
            ToolName tool,
            TargetType targetType,
            ToolArguments<A> validated,
            ExecutionContext context,
            ToolCapability<A, T> capability,
            InvocationTrace trace) {

        String targetId = resolveTarget(validated, context);
        trace.observe(context, targetId);

        ToolAuthorizationRequest request = ToolAuthorizationRequest.of(
                context, tool, targetType, targetId);

        return policyClient.authorize(request)
                .doOnNext(trace::observe)
                .flatMap(decision -> decision.permitted()
                        ? capability.invoke(validated.value(), context, decision)
                        : Mono.error(new ToolAuthorizationException(tool, decision.reasonCode())));
    }

    /**
     * The resource policy is asked about. A tool whose identity argument was om […]
     * targets the acting user, which is only knowable once the context exists.
     */
    private static String resolveTarget(ToolArguments<?> validated, ExecutionContext context) {
        if (!validated.targetIsCaller()) {
            return validated.targetId();
        }
        return context.subject().map(subject -> subject.domainId().value()).orElse(null);
    }

    private <T> void auditSuccess(InvocationTrace trace, T result, ToIntFunction<T> resultCount) {
        if (result == null) {
            return;
        }
        Duration elapsed = trace.elapsed();
        auditLogger.record(trace.event()
                .outcome(AuditOutcome.SUCCESS)
                .durationMillis(elapsed.toMillis())
                .resultCount(resultCount.applyAsInt(result))
                .build());
        metrics.recordInvocation(trace.tool, AuditOutcome.SUCCESS, elapsed);
    }

    private void auditFailure(InvocationTrace trace, ToolExecutionException error) {
        Duration elapsed = trace.elapsed();
        AuditOutcome outcome = error.category() == ErrorCategory.FORBIDDEN
                ? AuditOutcome.DENIED
                : AuditOutcome.FAILED;

        auditLogger.record(trace.event()
                .outcome(outcome)
                .durationMillis(elapsed.toMillis())
                .errorCategory(Optional.of(error.category()))
                .reasonCode(Optional.of(error.reasonCode()))
                .resultCount(0)
                .build());

        metrics.recordInvocation(trace.tool, outcome, elapsed);
        if (outcome == AuditOutcome.DENIED) {
            metrics.recordDenial(trace.tool, error.reasonCode());
        }
        if (UPSTREAM_CATEGORIES.contains(error.category())) {
            metrics.recordUpstreamFailure(trace.tool, error.category());
        }
    }

    // Reconstructed from the visible exception types and the documented error categories.
    private static ToolExecutionException normalize(Throwable error) {
        if (error instanceof ToolExecutionException toolError) {
            return toolError;
        }
        if (error instanceof TimeoutException) {
            return new UpstreamUnavailableException(ErrorCategory.TIMEOUT, "timeout", error);
        }
        if (error instanceof WebClientRequestException || error instanceof WebClientResponseException) {
            return new UpstreamUnavailableException(ErrorCategory.UPSTREAM_FAILURE, "upstream_failure", error);
        }
        return new InternalToolException(error);
    }





    /**
     * One invocation's audit context. […]

     */
    private static final class InvocationTrace {

        private final long startedAt = System.nanoTime();
        private String correlationId;
        private final ToolName tool;
        private final TargetType targetType;
        private String requestId = "unassigned";
        private String workload = "unauthenticated";
        private AuthorizationDecision decision = AuthorizationDecision.NOT_EVALUATED;
        private Optional<String> policyVersion = Optional.empty();
        private boolean subjectPresent;
        private SubjectSource subjectSource = SubjectSource.NONE;
        private Optional<String> delegatingActor = Optional.empty();
        private boolean selfService;

        private InvocationTrace(String correlationId, ToolName tool, TargetType targetType) {
            this.correlationId = correlationId;
            this.tool = tool;
            this.targetType = targetType;
        }

        private void observe(ExecutionContext context, String targetId) {
            // Prefer the id the provider resolved. The context-view value is th […]
            // Fa!Ture that happens hefore any execut!on context exists.
            if (!context.correlationId().isBlank()) {
                this.correlationId = context.correlationId();
            }
            this.requestId = context.requestId();
            this.workload = context.workload().clientId();
            this.subjectPresent = context.subject().isPresent();
            this.subjectSource = context.subjectSource();
            this.delegatingActor = context.delegatingActor();
            this.selfService = targetId != null && context.isSelfServiceFor(targetId);
        }

        private void observe(ToolAuthorizationDecision authorization) {
            this.decision = authorization.permitted()
                    ? AuthorizationDecision.PERMIT
                    : AuthorizationDecision.DENY;
            this.policyVersion = Optional.ofNullable(authorization.policyVersion());
        }

        private Duration elapsed() {
            return Duration.ofNanos(System.nanoTime() - startedAt);
        }

        private AuditEvent.AuditEventBuilder event() {
            return AuditEvent.builder()
                    .correlationId(correlationId)
                    .requestId(requestId)
                    .workloadClientId(workload)
                    .tool(tool)
                    .toolVersion(ToolVersion.CURRENT)
                    .decision(decision)
                    .policyVersion(policyVersion)
                    .targetType(targetType)
                    .subjectPresent(subjectPresent)
                    .subjectSource(subjectSource)
                    .delegatingActor(delegatingActor)
                    .selfService(selfService);
        }
    }
}
