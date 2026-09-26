package io.traceflow.core;

/**
 * Kubernetes/OpenShift pod identity, resolved from environment variables that are
 * normally populated via the Downward API (Section 10). Every field is optional:
 * outside a cluster (e.g. running locally) {@link #inCluster()} is {@code false}
 * and the remaining fields are empty strings rather than null, so callers can
 * attach them to telemetry unconditionally without null-checking each one.
 */
public record KubernetesMetadata(
        boolean inCluster,
        String podName,
        String podNamespace,
        String podIp,
        String nodeName,
        String containerName
) {

    private static final KubernetesMetadata NOT_IN_CLUSTER =
            new KubernetesMetadata(false, "", "", "", "", "");

    public static KubernetesMetadata notInCluster() {
        return NOT_IN_CLUSTER;
    }
}
