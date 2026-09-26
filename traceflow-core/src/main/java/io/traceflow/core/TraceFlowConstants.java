package io.traceflow.core;

/**
 * Well-known keys shared across the core, autoconfigure, and starter modules.
 * Centralized here so MDC keys, header names, and env var names never drift
 * between the logging pipeline and the tracing pipeline.
 */
public final class TraceFlowConstants {

    // MDC keys
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_SERVICE_NAME = "service";
    public static final String MDC_ENVIRONMENT = "environment";

    // W3C Trace Context headers (Section 8: no proprietary propagation format)
    public static final String HEADER_TRACEPARENT = "traceparent";
    public static final String HEADER_TRACESTATE = "tracestate";

    // Correlation id fallback header for callers that don't yet speak traceparent
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    // Kubernetes Downward API / env var names (Section 10)
    public static final String ENV_KUBERNETES_SERVICE_HOST = "KUBERNETES_SERVICE_HOST";
    public static final String ENV_POD_NAME = "POD_NAME";
    public static final String ENV_POD_NAMESPACE = "POD_NAMESPACE";
    public static final String ENV_POD_IP = "POD_IP";
    public static final String ENV_NODE_NAME = "NODE_NAME";
    public static final String ENV_CONTAINER_NAME = "CONTAINER_NAME";

    // Resource attribute names (OpenTelemetry semantic conventions)
    public static final String ATTR_SERVICE_NAME = "service.name";
    public static final String ATTR_SERVICE_VERSION = "service.version";
    public static final String ATTR_DEPLOYMENT_ENVIRONMENT = "deployment.environment";
    public static final String ATTR_K8S_NAMESPACE = "k8s.namespace.name";
    public static final String ATTR_K8S_POD_NAME = "k8s.pod.name";
    public static final String ATTR_K8S_CONTAINER_NAME = "k8s.container.name";

    public static final String UNKNOWN_SERVICE_NAME = "unknown-service";
    public static final String DEFAULT_ENVIRONMENT = "local";
    public static final String DEFAULT_OTLP_ENDPOINT = "http://localhost:4317";

    private TraceFlowConstants() {
    }
}
