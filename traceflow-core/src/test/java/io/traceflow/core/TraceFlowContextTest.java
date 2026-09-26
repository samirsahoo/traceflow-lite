package io.traceflow.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFlowContextTest {

    @AfterEach
    void resetState() {
        TraceFlowContext.clearCorrelationId();
        TraceFlowContext.init(TraceFlowConfiguration.defaults());
    }

    @Test
    void currentTraceAndSpanIdAreEmptyWithoutAnActiveSpan() {
        assertThat(TraceFlowContext.currentTraceId()).isEmpty();
        assertThat(TraceFlowContext.currentSpanId()).isEmpty();
    }

    @Test
    void fallsBackToAGeneratedStableCorrelationIdWithoutAnActiveSpan() {
        String first = TraceFlowContext.currentCorrelationId();
        String second = TraceFlowContext.currentCorrelationId();

        assertThat(first).isNotBlank().isEqualTo(second);
    }

    @Test
    void explicitCorrelationIdOverridesTheGeneratedOne() {
        TraceFlowContext.setCorrelationId("explicit-id");

        assertThat(TraceFlowContext.currentCorrelationId()).isEqualTo("explicit-id");
    }

    @Test
    void storesAndReturnsTheInitializedConfiguration() {
        TraceFlowConfiguration config = new TraceFlowConfiguration(
                true, "order-service", "prod", true, true, true,
                "http://otel:4317", KubernetesMetadata.notInCluster());

        TraceFlowContext.init(config);

        assertThat(TraceFlowContext.configuration().serviceName()).isEqualTo("order-service");
        assertThat(TraceFlowContext.configuration().environmentName()).isEqualTo("prod");
    }
}
