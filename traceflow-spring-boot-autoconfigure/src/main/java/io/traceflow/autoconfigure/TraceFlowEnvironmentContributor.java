package io.traceflow.autoconfigure;

import io.traceflow.core.KubernetesMetadata;
import io.traceflow.core.TraceFlowConfiguration;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds TraceFlow's resolved identity (service name, environment, pod/node
 * identity) under the {@code traceflow} key of {@code /actuator/info}, so an
 * operator can confirm what TraceFlow believes it's running as without cross
 * referencing Grafana.
 */
public class TraceFlowEnvironmentContributor implements InfoContributor {

    private final TraceFlowConfiguration configuration;

    public TraceFlowEnvironmentContributor(TraceFlowConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("service", configuration.serviceName());
        details.put("environment", configuration.environmentName());

        KubernetesMetadata k8s = configuration.kubernetesMetadata();
        if (k8s.inCluster()) {
            Map<String, Object> kubernetes = new LinkedHashMap<>();
            kubernetes.put("podName", k8s.podName());
            kubernetes.put("podNamespace", k8s.podNamespace());
            kubernetes.put("nodeName", k8s.nodeName());
            kubernetes.put("containerName", k8s.containerName());
            details.put("kubernetes", kubernetes);
        }

        builder.withDetail("traceflow", details);
    }
}
