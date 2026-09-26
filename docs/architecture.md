# Architecture

An interactive, explorable diagram of everything below - pan/zoom, guided
views per signal (traces/metrics/logs), light/dark - is at
[architecture-diagram.html](architecture-diagram.html). Generated with the
`archify` skill from `docs/architecture-diagram.json`; regenerate after any
real change to the signal paths with:

```bash
node .agents/skills/archify/bin/archify.mjs deliver architecture \
  docs/architecture-diagram.json docs/architecture-diagram.html --quality showcase
```

## The ten principles (Section 30)

Everything below is a consequence of these, so they're worth stating first:

1. Don't reinvent OpenTelemetry.
2. Don't create proprietary telemetry formats.
3. Observability must not affect business availability.
4. Prefer configuration over code.
5. Minimize dependencies.
6. Avoid high-cardinality metrics.
7. Keep the application-side library thin.
8. Use standard Kubernetes patterns.
9. Keep vendor neutrality.
10. Make every component independently replaceable.

## Signal-by-signal design

### Traces

TraceFlow does **not** build its own OpenTelemetry SDK bootstrap. Spring Boot
Actuator already ships one - `org.springframework.boot.actuate.autoconfigure
.tracing.OpenTelemetryAutoConfiguration` builds an `SdkTracerProvider`, an
OTLP exporter, a Micrometer bridge (`OtelTracer`, `OtelPropagator`), and
`Slf4JEventListener`/`Slf4JBaggageEventListener` for MDC correlation - the
moment `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp`
are on the classpath (which the starter provides).

TraceFlow's `traceflow-spring-boot-autoconfigure` module's job is narrower:
translate the friendly `traceflow.*` properties (Section 6) onto the native
`management.opentelemetry.*` / `management.tracing.*` / `management.otlp
.tracing.*` properties Boot's autoconfiguration reads, via a `TraceFlow
EnvironmentPostProcessor`. This has to run as an `EnvironmentPostProcessor`
(not a `@Bean`) because the properties it writes are read by *other*
auto-configuration classes' `@Conditional` checks, which are evaluated
before any `@Bean` method runs.

This was not the first design tried. An earlier iteration hand-built the
OTel SDK directly (`OpenTelemetrySdk.builder()...`) and wired the Micrometer
bridge manually. It worked, but building it surfaced that Spring Boot
Actuator's own `tracing.OpenTelemetryAutoConfiguration` does exactly the
same thing - already tested, already maintained upstream - and having both
active at once produced duplicate `Propagator`/`Tracer` beans. Deleting the
hand-rolled version in favor of bridging onto Boot's own auto-configuration
is a direct application of Principle 1.

**Why OTLP/HTTP, not OTLP/gRPC:** Spring Boot Actuator's `OtlpAutoConfiguration`
for tracing uses `OtlpHttpSpanExporter`, whose documented default endpoint is
`http://localhost:4318/v1/traces` - port 4318, not the gRPC convention of
4317. TraceFlow's default follows Boot's own default rather than fighting
it, and `traceflow-collector/config.yaml` accepts both protocols on both
ports, so nothing needs to change if a future version switches.

### Metrics

Micrometer + `micrometer-registry-prometheus`, scraped by Prometheus at
`/actuator/prometheus`. Metrics do **not** flow through the OTel Collector:
Prometheus's pull model is the standard, mature path for this signal, and
routing it through the collector would add a translation hop for no benefit
in this MVP. TraceFlow adds exactly two common tags (`service`,
`environment`) plus a `MeterFilter` that denies any meter carrying a known
high-cardinality tag key, as a belt-and-braces guard for Principle 6.

### Logs

Structured JSON on stdout (`logstash-logback-encoder`), with sensitive
fields masked via `MaskingJsonGeneratorDecorator`. `traceId`/`spanId` land in
MDC automatically because the tracing bridge's `Slf4JEventListener` puts
them there on every span scope open/close - TraceFlow's logging
configuration only wires the JSON encoder/appender, not the correlation
itself.

Logs reach Loki via Promtail tailing container stdout (Docker Compose) or
however the cluster's own logging pipeline is set up (Kubernetes) - not via
OTLP logs, which is a comparatively less mature signal across the
ecosystem than traces and metrics.

### Kubernetes metadata

`TraceFlowUtils.detectKubernetesMetadata()` reads `KUBERNETES_SERVICE_HOST`
(presence = in-cluster) plus `POD_NAME`/`POD_NAMESPACE`/`POD_IP`/
`NODE_NAME`/`CONTAINER_NAME` from the Downward API (Section 10), and
attaches `k8s.pod.name`/`k8s.namespace.name`/`k8s.container.name` as OTel
resource attributes.

## Failure isolation (Section 15)

- `BatchSpanProcessor` exports asynchronously, off the request thread, with
  a bounded queue - an unreachable collector means dropped spans, never a
  blocked request. Verified by `CollectorFailureIsolationTest`, which points
  the exporter at a closed port and asserts requests still complete in
  milliseconds.
- `TraceFlowHealthIndicator` never checks collector/Prometheus/Loki/Tempo
  reachability - it only reports TraceFlow's own configuration state.
- Prometheus scraping failing (app down) doesn't affect the app; the app's
  metrics registry failing doesn't affect Prometheus. They're independent
  processes by construction.

## Why core has no Spring dependency

`traceflow-core` depends only on `opentelemetry-api` and `slf4j-api`. It
exists so the correlation/masking/Kubernetes-metadata utilities are usable
from a Logback converter or a plain Java library that never touches Spring,
and so the autoconfigure module's job is provably just "bridge Spring
config onto this" rather than "contain the logic."
