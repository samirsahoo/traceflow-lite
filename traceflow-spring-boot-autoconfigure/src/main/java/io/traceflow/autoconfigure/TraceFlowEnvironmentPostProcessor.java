package io.traceflow.autoconfigure;

import io.traceflow.core.KubernetesMetadata;
import io.traceflow.core.TraceFlowUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridges the friendly {@code traceflow.*} properties (Section 6) onto the
 * native property namespace Spring Boot Actuator's own OpenTelemetry tracing
 * auto-configuration already reads: {@code management.opentelemetry.*},
 * {@code management.tracing.*}, {@code management.otlp.tracing.*}.
 *
 * <p>Per Principle 1 ("do not reinvent OpenTelemetry"), TraceFlow does not
 * build its own OTel SDK bridge - {@code spring-boot-actuator-autoconfigure}
 * already ships a complete one (SDK bootstrap, Micrometer bridge, MDC
 * correlation via {@code Slf4JEventListener}, W3C propagation) that activates
 * automatically once {@code micrometer-tracing-bridge-otel} and
 * {@code opentelemetry-exporter-otlp} are on the classpath, which the starter
 * already provides. This class only supplies the defaults, at the lowest
 * property-source precedence, so an application's own {@code management.*}
 * configuration always wins.
 *
 * <p>Must run as an {@link EnvironmentPostProcessor} (registered via
 * {@code META-INF/spring.factories}), not a {@code @Bean}: the properties it
 * writes are read by other auto-configuration classes' {@code @Conditional}
 * checks and {@code @ConfigurationProperties} binding, both of which happen
 * before any {@code @Bean} method could run.
 */
public class TraceFlowEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULT_OTLP_HTTP_ENDPOINT = "http://localhost:4318/v1/traces";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("traceflow.enabled", Boolean.class, true)) {
            return;
        }

        Map<String, Object> defaults = new LinkedHashMap<>();

        String serviceName = firstNonBlank(environment,
                "traceflow.service.name", "OTEL_SERVICE_NAME", "spring.application.name");
        if (serviceName != null) {
            defaults.put("spring.application.name", serviceName);
            defaults.put("management.opentelemetry.resource-attributes.service.name", serviceName);
        }

        String environmentName = TraceFlowUtils.defaultIfBlank(
                environment.getProperty("traceflow.environment.name"), "local");
        defaults.put("management.opentelemetry.resource-attributes.deployment.environment", environmentName);

        KubernetesMetadata k8s = TraceFlowUtils.detectKubernetesMetadata();
        if (k8s.inCluster()) {
            putIfNotBlank(defaults, "management.opentelemetry.resource-attributes.k8s.namespace.name", k8s.podNamespace());
            putIfNotBlank(defaults, "management.opentelemetry.resource-attributes.k8s.pod.name", k8s.podName());
            putIfNotBlank(defaults, "management.opentelemetry.resource-attributes.k8s.container.name", k8s.containerName());
        }

        boolean tracingEnabled = environment.getProperty("traceflow.tracing.enabled", Boolean.class, true);
        defaults.put("management.tracing.enabled", tracingEnabled);
        if (tracingEnabled) {
            String otlpEndpoint = firstNonBlank(environment, "traceflow.otlp.endpoint", "OTEL_EXPORTER_OTLP_ENDPOINT");
            defaults.put("management.otlp.tracing.endpoint", toTracesPath(
                    TraceFlowUtils.defaultIfBlank(otlpEndpoint, DEFAULT_OTLP_HTTP_ENDPOINT)));
            String samplingProbability = environment.getProperty("traceflow.tracing.sampling-probability");
            if (samplingProbability != null) {
                defaults.put("management.tracing.sampling.probability", samplingProbability);
            }
        }

        boolean metricsEnabled = environment.getProperty("traceflow.metrics.enabled", Boolean.class, true);
        defaults.put("management.metrics.export.prometheus.enabled", metricsEnabled);

        environment.getPropertySources().addLast(new MapPropertySource("traceflowDefaults", defaults));
    }

    private static String firstNonBlank(Environment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (!TraceFlowUtils.isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (!TraceFlowUtils.isBlank(value)) {
            map.put(key, value);
        }
    }

    /**
     * Accepts either a bare OTLP/HTTP base URL ({@code http://host:4318}, matching
     * the short form developers naturally write) or the full traces path, and
     * always returns the latter, since {@code OtlpHttpSpanExporter} requires it.
     */
    private static String toTracesPath(String endpoint) {
        String trimmed = endpoint.replaceAll("/+$", "");
        return trimmed.endsWith("/v1/traces") ? trimmed : trimmed + "/v1/traces";
    }
}
