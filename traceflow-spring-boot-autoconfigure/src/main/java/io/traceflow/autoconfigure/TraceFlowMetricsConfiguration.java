package io.traceflow.autoconfigure;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.traceflow.core.TraceFlowConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Section 7: JVM, HTTP, and application metrics via Micrometer, exposed for
 * Prometheus to scrape at {@code /actuator/prometheus}. TraceFlow adds only two
 * low-cardinality common tags (service, environment); JVM/HTTP metric binding
 * itself is Spring Boot Actuator's own auto-configuration - not duplicated here,
 * per Principle 1 (don't reinvent) and Principle 6 (avoid high-cardinality labels).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "traceflow.metrics", name = "enabled", matchIfMissing = true)
class TraceFlowMetricsConfiguration {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> traceFlowCommonTags(TraceFlowConfiguration configuration) {
        return registry -> registry.config()
                .commonTags("service", configuration.serviceName(), "environment", configuration.environmentName())
                // Belt-and-braces guard for Section 7: reject any meter carrying one of the
                // known high-cardinality identifier tag keys, even if a future instrumentation
                // point adds one by mistake.
                .meterFilter(new MeterFilter() {
                    @Override
                    public MeterFilterReply accept(Meter.Id id) {
                        boolean hasHighCardinalityTag = id.getTags().stream()
                                .anyMatch(tag -> isHighCardinalityTagKey(tag.getKey()));
                        return hasHighCardinalityTag ? MeterFilterReply.DENY : MeterFilterReply.NEUTRAL;
                    }
                });
    }

    private static boolean isHighCardinalityTagKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("requestid") || normalized.contains("userid")
                || normalized.contains("transactionid") || normalized.contains("email")
                || normalized.equals("uri_raw");
    }
}
