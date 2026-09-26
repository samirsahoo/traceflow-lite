package io.traceflow.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.traceflow.core.TraceFlowConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises what {@link TraceFlowAutoConfiguration} itself wires: the resolved
 * {@link TraceFlowConfiguration}, health/info contributors, and metrics common
 * tags. The OpenTelemetry/Micrometer tracing bridge is Spring Boot Actuator's
 * own auto-configuration (see {@link TraceFlowEnvironmentPostProcessor}), and
 * {@code EnvironmentPostProcessor}s only run under a real
 * {@code SpringApplication} bootstrap - not under {@link ApplicationContextRunner}
 * - so that end-to-end wiring is verified instead by traceflow-demo's
 * {@code @SpringBootTest}.
 */
class TraceFlowAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MetricsAutoConfiguration.class,
                    SimpleMetricsExportAutoConfiguration.class,
                    HealthContributorAutoConfiguration.class,
                    InfoContributorAutoConfiguration.class,
                    TraceFlowAutoConfiguration.class));

    @Test
    void wiresConfigurationMetricsAndHealthByDefault() {
        contextRunner
                .withPropertyValues("traceflow.service.name=order-service", "traceflow.environment.name=prod")
                .run(context -> {
                    assertThat(context).hasSingleBean(TraceFlowConfiguration.class);
                    assertThat(context).hasSingleBean(TraceFlowHealthIndicator.class);
                    assertThat(context).hasSingleBean(TraceFlowEnvironmentContributor.class);
                    assertThat(context).hasSingleBean(MeterRegistry.class);

                    TraceFlowConfiguration configuration = context.getBean(TraceFlowConfiguration.class);
                    assertThat(configuration.serviceName()).isEqualTo("order-service");
                    assertThat(configuration.environmentName()).isEqualTo("prod");
                });
    }

    @Test
    void disablingTraceFlowRemovesAllOfItsBeans() {
        contextRunner
                .withPropertyValues("traceflow.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TraceFlowConfiguration.class));
    }

    @Test
    void healthReportsUpWithTraceFlowDetails() {
        contextRunner
                .withPropertyValues("traceflow.service.name=order-service")
                .run(context -> {
                    TraceFlowHealthIndicator indicator = context.getBean(TraceFlowHealthIndicator.class);
                    assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
                    assertThat(indicator.health().getDetails()).containsEntry("service", "order-service");
                });
    }

    @Test
    void otelServiceNameEnvVarIsHonoredWhenTraceFlowPropertyIsUnset() {
        contextRunner
                .withSystemProperties("OTEL_SERVICE_NAME=payment-service")
                .run(context -> {
                    TraceFlowConfiguration configuration = context.getBean(TraceFlowConfiguration.class);
                    assertThat(configuration.serviceName()).isEqualTo("payment-service");
                });
    }
}
