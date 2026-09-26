package io.traceflow.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFlowUtilsTest {

    @Test
    void detectsInClusterMetadataFromEnvVars() {
        Map<String, String> env = Map.of(
                TraceFlowConstants.ENV_KUBERNETES_SERVICE_HOST, "10.0.0.1",
                TraceFlowConstants.ENV_POD_NAME, "payment-service-abc123",
                TraceFlowConstants.ENV_POD_NAMESPACE, "prod",
                TraceFlowConstants.ENV_NODE_NAME, "node-1",
                TraceFlowConstants.ENV_CONTAINER_NAME, "app"
        );

        KubernetesMetadata metadata = TraceFlowUtils.detectKubernetesMetadata(env::get);

        assertThat(metadata.inCluster()).isTrue();
        assertThat(metadata.podName()).isEqualTo("payment-service-abc123");
        assertThat(metadata.podNamespace()).isEqualTo("prod");
        assertThat(metadata.nodeName()).isEqualTo("node-1");
        assertThat(metadata.containerName()).isEqualTo("app");
    }

    @Test
    void returnsNotInClusterWhenServiceHostMissing() {
        KubernetesMetadata metadata = TraceFlowUtils.detectKubernetesMetadata(key -> null);

        assertThat(metadata.inCluster()).isFalse();
        assertThat(metadata.podName()).isEmpty();
    }

    @Test
    void identifiesSensitiveKeysCaseAndSeparatorInsensitively() {
        assertThat(TraceFlowUtils.isSensitiveKey("Authorization")).isTrue();
        assertThat(TraceFlowUtils.isSensitiveKey("X-Api-Key")).isTrue();
        assertThat(TraceFlowUtils.isSensitiveKey("user_password")).isTrue();
        assertThat(TraceFlowUtils.isSensitiveKey("credit-card-number")).isTrue();
        assertThat(TraceFlowUtils.isSensitiveKey("Content-Type")).isFalse();
    }

    @Test
    void masksOnlySensitiveEntriesAndLeavesOthersUntouched() {
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer eyJhbGciOi...",
                "Content-Type", "application/json"
        );

        Map<String, String> masked = TraceFlowUtils.maskSensitiveEntries(headers);

        assertThat(masked.get("Authorization")).isEqualTo("***");
        assertThat(masked.get("Content-Type")).isEqualTo("application/json");
    }

    @Test
    void generatesNonBlankUniqueCorrelationIds() {
        String first = TraceFlowUtils.generateCorrelationId();
        String second = TraceFlowUtils.generateCorrelationId();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(first).isNotEqualTo(second);
    }
}
