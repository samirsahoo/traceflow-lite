package io.traceflow.core;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * Process-wide access to the resolved TraceFlow configuration and the
 * currently active trace/span/correlation identifiers.
 *
 * <p>This is intentionally a static holder, not an injected bean: it must be
 * reachable from places that are not Spring-managed, most notably the Logback
 * {@code MDC} and any custom encoder/converter running in the logging pipeline
 * before a request's MDC is populated. Spring wiring (in the autoconfigure
 * module) calls {@link #init(TraceFlowConfiguration)} once at startup.
 */
public final class TraceFlowContext {

    private static volatile TraceFlowConfiguration configuration = TraceFlowConfiguration.defaults();

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private TraceFlowContext() {
    }

    public static void init(TraceFlowConfiguration config) {
        configuration = config;
    }

    public static TraceFlowConfiguration configuration() {
        return configuration;
    }

    /** The active W3C trace id, or "" when there is no valid active span. */
    public static String currentTraceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        return spanContext.isValid() ? spanContext.getTraceId() : "";
    }

    /** The active W3C span id, or "" when there is no valid active span. */
    public static String currentSpanId() {
        SpanContext spanContext = Span.current().getSpanContext();
        return spanContext.isValid() ? spanContext.getSpanId() : "";
    }

    /**
     * The identifier to use for cross-cutting correlation in logs. Prefers the
     * active trace id (so logs, metrics and traces line up); falls back to a
     * per-thread generated id for code paths with tracing disabled or not yet
     * inside a span (e.g. very early request filters, scheduled jobs).
     */
    public static String currentCorrelationId() {
        String traceId = currentTraceId();
        if (!traceId.isEmpty()) {
            return traceId;
        }
        String threadLocalId = CORRELATION_ID.get();
        if (threadLocalId == null) {
            threadLocalId = TraceFlowUtils.generateCorrelationId();
            CORRELATION_ID.set(threadLocalId);
        }
        return threadLocalId;
    }

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static void clearCorrelationId() {
        CORRELATION_ID.remove();
    }
}
