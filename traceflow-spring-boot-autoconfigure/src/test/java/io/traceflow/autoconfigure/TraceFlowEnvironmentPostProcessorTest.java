package io.traceflow.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFlowEnvironmentPostProcessorTest {

    private final TraceFlowEnvironmentPostProcessor postProcessor = new TraceFlowEnvironmentPostProcessor();

    @Test
    void bridgesServiceNameOntoSpringApplicationNameAndResourceAttributes() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("traceflow.service.name", "order-service");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.application.name")).isEqualTo("order-service");
        assertThat(environment.getProperty("management.opentelemetry.resource-attributes.service.name"))
                .isEqualTo("order-service");
    }

    @Test
    void defaultsToOtlpHttpEndpointWithTracesPathWhenNothingConfigured() {
        MockEnvironment environment = new MockEnvironment();

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://localhost:4318/v1/traces");
    }

    @Test
    void appendsTheTracesPathToABareOtlpEndpoint() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("traceflow.otlp.endpoint", "http://traceflow-collector:4318");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://traceflow-collector:4318/v1/traces");
    }

    @Test
    void doesNotDoubleAppendWhenTheFullPathIsAlreadyGiven() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("traceflow.otlp.endpoint", "http://traceflow-collector:4318/v1/traces");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://traceflow-collector:4318/v1/traces");
    }

    @Test
    void disablingTracingPropagatesToManagementTracingEnabledAndSkipsTheOtlpEndpoint() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("traceflow.tracing.enabled", "false");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("management.tracing.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.otlp.tracing.endpoint")).isNull();
    }

    @Test
    void doesNothingWhenTraceFlowItselfIsDisabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("traceflow.enabled", "false");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("management.tracing.enabled")).isNull();
    }

    @Test
    void applicationOwnedManagementPropertiesTakePrecedenceOverTraceFlowDefaults() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("traceflow.otlp.endpoint", "http://traceflow-collector:4318");
        environment.setProperty("management.otlp.tracing.endpoint", "http://custom-collector:4318/v1/traces");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .isEqualTo("http://custom-collector:4318/v1/traces");
    }
}
