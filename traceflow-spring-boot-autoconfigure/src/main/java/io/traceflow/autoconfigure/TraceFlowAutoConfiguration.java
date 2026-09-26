package io.traceflow.autoconfigure;

import io.traceflow.core.TraceFlowConfiguration;
import io.traceflow.core.TraceFlowConstants;
import io.traceflow.core.TraceFlowContext;
import io.traceflow.core.TraceFlowUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Root auto-configuration (Section 6). Resolves the framework-agnostic
 * {@link TraceFlowConfiguration}, publishes it via {@link TraceFlowContext} so
 * non-Spring code (Logback converters, static utilities) can reach it, and
 * imports the metrics/tracing/logging sub-configurations.
 *
 * <p>Everything here is gated by {@code traceflow.enabled} (default true), and
 * each sub-configuration is independently gated by its own
 * {@code traceflow.<feature>.enabled} flag, so a single dependency lets a
 * developer opt out of any one signal without forking the starter.
 */
@AutoConfiguration
@EnableConfigurationProperties(TraceFlowProperties.class)
@ConditionalOnProperty(prefix = "traceflow", name = "enabled", matchIfMissing = true)
@org.springframework.context.annotation.Import({
        TraceFlowMetricsConfiguration.class,
        TraceFlowLoggingConfiguration.class
})
public class TraceFlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceFlowConfiguration traceFlowConfiguration(TraceFlowProperties properties, Environment environment) {
        TraceFlowConfiguration config = new TraceFlowConfiguration(
                properties.isEnabled(),
                resolveServiceName(properties, environment),
                resolveEnvironmentName(properties, environment),
                properties.getTracing().isEnabled(),
                properties.getMetrics().isEnabled(),
                properties.getLogging().isEnabled(),
                resolveOtlpEndpoint(properties, environment),
                TraceFlowUtils.detectKubernetesMetadata()
        );
        TraceFlowContext.init(config);
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceFlowHealthIndicator traceFlowHealthIndicator(TraceFlowConfiguration configuration) {
        return new TraceFlowHealthIndicator(configuration);
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceFlowEnvironmentContributor traceFlowEnvironmentContributor(TraceFlowConfiguration configuration) {
        return new TraceFlowEnvironmentContributor(configuration);
    }

    /**
     * Resolution order: explicit {@code traceflow.service.name} (yml or
     * {@code TRACEFLOW_SERVICE_NAME} env var) &gt; OpenTelemetry-native
     * {@code OTEL_SERVICE_NAME} &gt; Spring's own {@code spring.application.name}
     * &gt; the property default ("unknown-service").
     */
    private static String resolveServiceName(TraceFlowProperties properties, Environment environment) {
        String explicit = environment.getProperty("traceflow.service.name");
        if (!TraceFlowUtils.isBlank(explicit)) {
            return explicit;
        }
        String otelNative = environment.getProperty("OTEL_SERVICE_NAME");
        if (!TraceFlowUtils.isBlank(otelNative)) {
            return otelNative;
        }
        String springAppName = environment.getProperty("spring.application.name");
        if (!TraceFlowUtils.isBlank(springAppName)) {
            return springAppName;
        }
        return TraceFlowUtils.defaultIfBlank(properties.getService().getName(), TraceFlowConstants.UNKNOWN_SERVICE_NAME);
    }

    private static String resolveEnvironmentName(TraceFlowProperties properties, Environment environment) {
        String explicit = environment.getProperty("traceflow.environment.name");
        if (!TraceFlowUtils.isBlank(explicit)) {
            return explicit;
        }
        return TraceFlowUtils.defaultIfBlank(properties.getEnvironment().getName(), TraceFlowConstants.DEFAULT_ENVIRONMENT);
    }

    /**
     * Resolution order: explicit {@code traceflow.otlp.endpoint} &gt; the
     * OpenTelemetry-native {@code OTEL_EXPORTER_OTLP_ENDPOINT} &gt; the property
     * default. This is the value shown by the health/info endpoints; the value
     * actually used by the exporter is derived independently in
     * {@link TraceFlowEnvironmentPostProcessor}, which also normalizes a bare
     * {@code host:4318} into the full {@code /v1/traces} path OTLP/HTTP needs.
     */
    private static String resolveOtlpEndpoint(TraceFlowProperties properties, Environment environment) {
        String explicit = environment.getProperty("traceflow.otlp.endpoint");
        if (!TraceFlowUtils.isBlank(explicit)) {
            return explicit;
        }
        String otelNative = environment.getProperty("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (!TraceFlowUtils.isBlank(otelNative)) {
            return otelNative;
        }
        return TraceFlowUtils.defaultIfBlank(properties.getOtlp().getEndpoint(), TraceFlowConstants.DEFAULT_OTLP_ENDPOINT);
    }
}
