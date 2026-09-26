# Kubernetes Deployment

## Plain manifests

```bash
kubectl apply -f deployment/kubernetes/
```

This creates the `traceflow-lite` namespace and:

| File | Creates |
|---|---|
| `namespace.yaml` | The `traceflow-lite` namespace |
| `serviceaccount.yaml` | A service account for the demo app (token auto-mount disabled - it doesn't call the Kubernetes API) |
| `configmap.yaml` | Demo app env vars + the OTel Collector's config (mirrors `traceflow-collector/config.yaml`) |
| `deployment.yaml` | The demo app Deployment (2 replicas) + Service |
| `otel-collector.yaml` | The collector Deployment (1 replica) + Service |

You'll need to build and push the demo image yourself first (or load it
into your cluster's local registry for kind/minikube):

```bash
mvn -pl traceflow-demo -am package
docker build -f deployment/docker/Dockerfile -t traceflow-demo:0.1.0 .
```

## Helm

```bash
helm install traceflow deployment/helm/traceflow-lite
```

By default this installs **only** the demo app and the OTel Collector - it
assumes Prometheus/Loki/Tempo/Grafana already exist in your cluster (or
that you're pointing at an existing observability stack). To have the
chart install all of them too, via their real upstream charts:

```bash
helm dependency update deployment/helm/traceflow-lite
helm install traceflow deployment/helm/traceflow-lite \
  --set prometheus.enabled=true --set grafana.enabled=true \
  --set loki.enabled=true --set tempo.enabled=true
```

See `deployment/helm/traceflow-lite/Chart.yaml` for exactly which upstream
chart versions are pinned.

## Downward API / pod identity

Every pod gets `POD_NAME`, `POD_NAMESPACE`, `POD_IP`, `NODE_NAME` injected
via `fieldRef` (Section 10) - TraceFlow reads these automatically and
attaches them as `k8s.pod.name`/`k8s.namespace.name`/etc. trace resource
attributes; no application code needed.

## Health probes

Spring Boot Actuator auto-detects the Kubernetes environment (via
`KUBERNETES_SERVICE_HOST`) and exposes `/actuator/health/liveness` and
`/actuator/health/readiness` automatically - the manifests wire these into
`livenessProbe`/`readinessProbe` directly; there's no TraceFlow-specific
probe configuration.

## Metrics scraping

If your cluster runs the Prometheus Operator, add a `ServiceMonitor`
pointed at the demo Service's `/actuator/prometheus` path (not included
here, to avoid a hard dependency on that CRD existing - see
[docs/troubleshooting.md](troubleshooting.md) if metrics aren't showing up
in your cluster's Prometheus).
