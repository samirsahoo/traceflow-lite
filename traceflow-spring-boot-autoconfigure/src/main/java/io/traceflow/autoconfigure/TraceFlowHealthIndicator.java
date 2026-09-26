package io.traceflow.autoconfigure;

import io.traceflow.core.TraceFlowConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Reports TraceFlow's own configuration state under
 * {@code /actuator/health/traceflow}. Intentionally never checks whether the
 * OTLP collector, Prometheus, Loki, or Tempo are reachable: per Section 14/15,
 * observability backend availability must never affect application health.
 */
public class TraceFlowHealthIndicator implements HealthIndicator {

    private final TraceFlowConfiguration configuration;

    public TraceFlowHealthIndicator(TraceFlowConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Health health() {
        Health.Builder builder = configuration.enabled() ? Health.up() : Health.down();
        return builder
                .withDetail("service", configuration.serviceName())
                .withDetail("environment", configuration.environmentName())
                .withDetail("tracing", configuration.tracingEnabled())
                .withDetail("metrics", configuration.metricsEnabled())
                .withDetail("logging", configuration.loggingEnabled())
                .withDetail("kubernetes", configuration.kubernetesMetadata().inCluster())
                .build();
    }
}
