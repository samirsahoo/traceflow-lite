# Grafana Dashboards

All three live in `grafana/dashboards/` and are provisioned automatically
via `grafana/provisioning/dashboards/dashboards.yaml` - no manual
"Add dashboard" step.

## Application Overview (`application-overview.json`)

Request rate, error rate, P95/P99 latency, HTTP 4xx/5xx rate, JVM heap,
process CPU, live thread count, GC pause rate. A `service` template
variable filters everything to one app at a time (defaults to
`traceflow-demo`). Fully populated by the local docker-compose stack.

## Kubernetes Application (`kubernetes-application.json`)

Pod count, restart count, container ready/waiting status, CPU per pod,
memory per pod, network rx/tx. Needs `kube-state-metrics` and cAdvisor
metrics in the cluster's Prometheus - **stays empty against the local
docker-compose stack by design**, since compose doesn't run a kubelet.
Populates once deployed to an actual Kubernetes/OpenShift cluster whose
Prometheus scrapes those two sources (most clusters' bundled monitoring
stack already does).

## Distributed Tracing (`distributed-tracing.json`)

Three Tempo TraceQL panels - Recent Traces (`{}`), Error Traces
(`{status=error}`), Slow Traces (`{duration > 1s}`) - plus a text panel
explaining how to enable the service graph (see
[docs/tracing.md](tracing.md#enabling-the-tempo-service-graph-optional)).

## Metric ↔ Trace ↔ Log navigation

Configured once, in `grafana/provisioning/datasources/datasources.yaml`:

- **Metric → Trace**: Prometheus exemplars carry a `traceId`; clicking one
  jumps to that trace in Tempo.
- **Trace → Logs**: Tempo's `tracesToLogsV2` jumps to Loki filtered by
  that trace's ID.
- **Logs → Trace**: Loki's `derivedFields` regex-extracts `"traceId":"..."`
  from the JSON log line and links back to Tempo.

## Editing a dashboard

Edit the JSON directly (they're small and readable), or edit in the
Grafana UI and export back to the file - either way, restart Grafana (or
wait `updateIntervalSeconds: 30`) to pick up the change. Keep to the "a
small number of useful dashboards" principle (Section 13) rather than
adding a new dashboard per feature.
