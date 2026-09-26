package io.traceflow.core;

import java.util.Objects;

/**
 * Immutable, fully-resolved TraceFlow configuration. This is the framework-agnostic
 * counterpart of the Spring {@code TraceFlowProperties} in the autoconfigure module:
 * autoconfigure translates Spring configuration properties into this object once,
 * and every core utility depends only on this - never on Spring - so the core module
 * stays usable outside a Spring application.
 */
public record TraceFlowConfiguration(
        boolean enabled,
        String serviceName,
        String environmentName,
        boolean tracingEnabled,
        boolean metricsEnabled,
        boolean loggingEnabled,
        String otlpEndpoint,
        KubernetesMetadata kubernetesMetadata
) {

    public TraceFlowConfiguration {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(environmentName, "environmentName must not be null");
        Objects.requireNonNull(otlpEndpoint, "otlpEndpoint must not be null");
        Objects.requireNonNull(kubernetesMetadata, "kubernetesMetadata must not be null");
    }

    public static TraceFlowConfiguration defaults() {
        return new TraceFlowConfiguration(
                true,
                TraceFlowConstants.UNKNOWN_SERVICE_NAME,
                TraceFlowConstants.DEFAULT_ENVIRONMENT,
                true,
                true,
                true,
                TraceFlowConstants.DEFAULT_OTLP_ENDPOINT,
                KubernetesMetadata.notInCluster()
        );
    }
}
