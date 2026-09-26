# Tracing

## How a span gets created

1. Spring Boot Actuator's `MicrometerTracingAutoConfiguration` activates
   automatically because a `io.micrometer.tracing.Tracer` bean exists (it's
   created by Boot's own `tracing.OpenTelemetryAutoConfiguration`, which
   activates because `micrometer-tracing-bridge-otel` and
   `opentelemetry-exporter-otlp` are on the classpath - both come from
   `traceflow-spring-boot-starter`).
2. Spring MVC's `ServerHttpObservationFilter` wraps every request in an
   `Observation`; because a `Tracer` bean exists, that Observation also
   creates a span.
3. Outbound `RestClient`/`RestTemplate`/`WebClient` calls are instrumented
   the same way, and because a `Propagator` bean also exists, the
   `traceparent` (and `tracestate`) headers are injected automatically -
   this is exactly how `examples/order-service` propagates its trace into
   `examples/payment-service` with zero manual header-handling code.
4. `Slf4JEventListener` (from `micrometer-tracing-bridge-otel`) puts
   `traceId`/`spanId` into SLF4J's MDC on every span scope open/close.

None of this is TraceFlow code - see
[docs/architecture.md](architecture.md) for exactly what TraceFlow itself
contributes (property translation + Kubernetes resource attributes).

## Propagation format

Strictly W3C Trace Context (`traceparent`/`tracestate`). No proprietary
propagation header exists anywhere in this project (Section 8, Principle 2).

## Sampling

```yaml
traceflow:
  tracing:
    sampling-probability: 0.1   # 0.0-1.0; 1.0 = sample everything
```

Maps directly to `management.tracing.sampling.probability`. TraceFlow does
not implement its own sampler.

## Resource attributes on every span

- `service.name` - from `traceflow.service.name` resolution (see
  [configuration.md](configuration.md))
- `deployment.environment` - from `traceflow.environment.name`
- `k8s.namespace.name`, `k8s.pod.name`, `k8s.container.name` - only present
  when running in a cluster (Section 10)

## Enabling the Tempo service graph (optional)

Not wired up by default - it requires Tempo's `metrics_generator` writing
span-derived metrics into Prometheus via remote-write. To enable:

1. Add to `tempo/tempo.yaml`:

   ```yaml
   metrics_generator:
     registry:
       external_labels:
         source: tempo
     storage:
       path: /var/tempo/generator/wal
       remote_write:
         - url: http://prometheus:9090/api/v1/write
           send_exemplars: true
   overrides:
     defaults:
       metrics_generator:
         processors: [service-graphs, span-metrics]
   ```

2. Start Prometheus with `--web.enable-remote-write-receiver`.
3. In `grafana/dashboards/distributed-tracing.json`, replace the "Service
   Graph" text panel with a `nodeGraph` panel using the Tempo datasource's
   `serviceMap` query type.

## Troubleshooting a missing trace

See [docs/troubleshooting.md](troubleshooting.md#traces-not-appearing-in-tempo).
