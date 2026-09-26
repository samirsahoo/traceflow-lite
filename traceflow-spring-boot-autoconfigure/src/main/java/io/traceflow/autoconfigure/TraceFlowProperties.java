package io.traceflow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Binds the {@code traceflow.*} configuration tree (Section 6). Field defaults
 * below are the last-resort fallback; {@link TraceFlowAutoConfiguration} additionally
 * honors the OpenTelemetry-native {@code OTEL_SERVICE_NAME} and
 * {@code OTEL_EXPORTER_OTLP_ENDPOINT} env vars as a secondary fallback when the
 * corresponding {@code traceflow.*} property was never explicitly set.
 */
@ConfigurationProperties(prefix = "traceflow")
public class TraceFlowProperties {

    /** Master switch. When false, no TraceFlow bean does any work. */
    private boolean enabled = true;

    @NestedConfigurationProperty
    private final Service service = new Service();

    @NestedConfigurationProperty
    private final Environment environment = new Environment();

    @NestedConfigurationProperty
    private final Metrics metrics = new Metrics();

    @NestedConfigurationProperty
    private final Tracing tracing = new Tracing();

    @NestedConfigurationProperty
    private final Logging logging = new Logging();

    @NestedConfigurationProperty
    private final Otlp otlp = new Otlp();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Service getService() {
        return service;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public Tracing getTracing() {
        return tracing;
    }

    public Logging getLogging() {
        return logging;
    }

    public Otlp getOtlp() {
        return otlp;
    }

    public static class Service {
        private String name = "unknown-service";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Environment {
        private String name = "local";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Metrics {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Tracing {
        private boolean enabled = true;
        /** Sampling probability, 0.0-1.0. Kept simple: parent-based ratio sampler only (Principle 7). */
        private double samplingProbability = 1.0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSamplingProbability() {
            return samplingProbability;
        }

        public void setSamplingProbability(double samplingProbability) {
            this.samplingProbability = samplingProbability;
        }
    }

    public static class Logging {
        private boolean enabled = true;
        private boolean jsonEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isJsonEnabled() {
            return jsonEnabled;
        }

        public void setJsonEnabled(boolean jsonEnabled) {
            this.jsonEnabled = jsonEnabled;
        }
    }

    public static class Otlp {
        private String endpoint = "http://localhost:4317";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
