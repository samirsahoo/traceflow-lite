# Troubleshooting

## Traces not appearing in Tempo

1. Confirm the app resolved an endpoint at all:
   `curl http://localhost:8080/actuator/info` and check the `traceflow`
   section - or just check startup logs for the OTLP exporter's target.
2. Confirm the collector is actually reachable from the app's network
   (in Docker Compose, both must be on the same compose network - the
   default `docker compose up` creates one automatically; in Kubernetes,
   confirm the `traceflow-collector` Service resolves via DNS from the
   app's pod: `kubectl exec <pod> -- getent hosts traceflow-collector`).
3. Remember the exporter is **OTLP/HTTP**, not gRPC - the endpoint must
   include `/v1/traces` (TraceFlow appends this automatically for a bare
   `host:port`, but a manually-set `management.otlp.tracing.endpoint`
   bypasses that normalization).
4. Check `management.tracing.sampling.probability` isn't `0.0` somewhere in
   your config, silently sampling everything out.
5. An unreachable collector fails **silently** by design (Section 15) - it
   will never throw, log an ERROR by default, or show up as a 500. Check
   the collector's own logs for `otlp` receiver activity, and Tempo's logs
   for ingestion activity, working backwards from Tempo toward the app.

## Metrics not appearing in Prometheus

1. `curl http://localhost:8080/actuator/prometheus` directly - if this is
   empty or 404, the issue is in the app, not Prometheus. A 404 usually
   means `management.endpoints.web.exposure.include` doesn't list
   `prometheus`.
2. Check Prometheus's own target status: http://localhost:9090/targets -
   confirm the `traceflow-demo` job is `UP`. If `DOWN`, check the network
   path between the Prometheus container and the app (in Kubernetes, add a
   `ServiceMonitor` if using the Prometheus Operator - see
   [docs/kubernetes.md](kubernetes.md)).
3. Percentile histogram panels (P95/P99) are empty specifically:  confirm
   `management.metrics.distribution.percentiles-histogram.http.server
   .requests=true` is set - without it, `http_server_requests_seconds_bucket`
   simply doesn't exist.

## Logs missing traceId/spanId

- Confirm `traceflow.tracing.enabled` isn't `false`.
- The correlation only appears on log lines emitted **while a span is
  active** - a log statement in a `@PostConstruct` method, or before the
  request enters Spring MVC's dispatcher, won't have one. This is expected.

## JSON logs not appearing (still seeing the default console pattern)

- Confirm `traceflow.logging.json-enabled` isn't `false`.
- Confirm `logstash-logback-encoder` and Logback are actually on the
  classpath (they come from the starter; a custom exclusion would remove
  them).
- Remember the one documented limitation: log lines from before the Spring
  context refreshes (the banner, very early startup lines) always use the
  default pattern - this is not a bug.

## Docker Compose stack won't start

This stack was written and JSON/YAML-validated in an environment without a
running Docker daemon, so it has not been runtime-verified end-to-end as
of this writing. If something fails on first run:

1. `docker compose logs <service>` for the specific failing container.
2. Config file mount issues are the most likely first failure - confirm
   `traceflow-collector/config.yaml`, `prometheus/prometheus.yml`,
   `loki/loki-config.yaml`, `tempo/tempo.yaml` are all present relative to
   wherever you ran `docker compose up` from (must be the repo root).
3. Promtail needs access to the Docker socket
   (`/var/run/docker.sock`) - on some platforms (rootless Docker, certain
   CI runners) this mount needs an explicit permission grant.

## OpenShift: pod fails to schedule / SCC denies it

See [docs/openshift.md](openshift.md#verifying-scc-compliance-yourself) -
almost always means something added a `runAsUser` back in, or a capability
wasn't dropped.
