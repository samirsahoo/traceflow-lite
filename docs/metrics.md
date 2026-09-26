# Metrics

## Where they come from

Micrometer + `micrometer-registry-prometheus`, exposed at
`/actuator/prometheus`. This is Spring Boot Actuator's own metrics
auto-configuration - TraceFlow does not bind JVM or HTTP metrics itself
(Principle 1). What TraceFlow adds:

- Two common tags on every meter: `service`, `environment`.
- A `MeterFilter` that denies (drops) any meter carrying a tag key matching
  `requestid`, `userid`, `transactionid`, `email`, or `uri_raw` - a
  belt-and-braces guard against a future instrumentation point accidentally
  introducing a high-cardinality label (Section 7, Principle 6).

## What's exposed out of the box

- **JVM**: `jvm_memory_used_bytes`, `jvm_gc_pause_seconds_*`,
  `jvm_threads_live_threads`, `jvm_classes_loaded_classes`, and more.
- **HTTP**: `http_server_requests_seconds_{count,sum,bucket}`, tagged with
  `method`, `uri` (the route template, e.g. `/api/orders/{id}` - never the
  resolved path with real IDs in it), `status`, `outcome`.
- **Process**: `process_cpu_usage`, `system_cpu_usage`.

Percentile histograms for `http.server.requests` (needed for the P95/P99
panels on the Application Overview dashboard) are enabled in
`traceflow-demo/src/main/resources/application.yml`:

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

Add the same to your own application if you want those panels populated.

## Avoiding high-cardinality metrics (Section 7)

Never add a raw request ID, user ID, transaction ID, email address, or
resolved URL as a metric tag. Spring's `WebMvcTagsProvider` already uses
the matched route *pattern* (not the resolved path) for the `uri` tag by
default, so `/api/orders/{id}` stays one series regardless of how many
distinct order IDs are requested - this is Spring Boot's own protection,
not something TraceFlow adds, but it's why the default behavior is already
safe.

## Metric → Trace (exemplars)

Because `micrometer-tracing-bridge-otel` is present, Spring Boot Actuator's
`PrometheusExemplarsAutoConfiguration` attaches the current trace ID as a
Prometheus exemplar on `Timer`/`Counter` meters automatically. Grafana's
Prometheus datasource is provisioned with `exemplarTraceIdDestinations`
pointed at Tempo (`grafana/provisioning/datasources/datasources.yaml`), so
clicking an exemplar dot on a graph jumps straight to that request's trace.
