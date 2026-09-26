# TraceFlow Lite

A lightweight, OpenTelemetry-native observability starter for Java/Spring Boot
applications on Kubernetes and OpenShift.

## What is TraceFlow Lite?

Add one dependency, set a service name, and get metrics, traces, structured
logs, and Kubernetes identity - all correlated by trace ID - without writing
any instrumentation code:

```xml
<dependency>
    <groupId>io.traceflow</groupId>
    <artifactId>traceflow-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
traceflow:
  service:
    name: payment-service
  otlp:
    endpoint: http://traceflow-collector:4317
```

## Why does it exist?

Most Java teams end up hand-wiring the same five things on every new
service: Micrometer, a tracing bridge, an OTLP exporter, JSON logging with
trace correlation, and Kubernetes pod metadata. TraceFlow Lite packages that
wiring once, as a thin layer on top of Spring Boot's own (excellent, already
OpenTelemetry-native) observability support - it does not reinvent
OpenTelemetry, Micrometer, or any storage backend. See
[docs/architecture.md](docs/architecture.md) for the full list of principles
this project holds itself to.

## Add TraceFlow Lite to Your Own Application

This walks through wiring TraceFlow Lite into a Spring Boot app you already
have - not this repo's demo. Five steps, no instrumentation code.

### 1. Build and install it locally

Not yet published to Maven Central - install the three library modules
into your local `~/.m2` repository first:

```bash
git clone <repo>
cd traceflow-lite
mvn -pl traceflow-core,traceflow-spring-boot-autoconfigure,traceflow-spring-boot-starter -am install -DskipTests
```

### 2. Add the one dependency

```xml
<dependency>
    <groupId>io.traceflow</groupId>
    <artifactId>traceflow-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Built and tested against **Spring Boot 3.5.16 / Java 21** - align your own
`spring-boot-starter-parent` version to avoid dependency conflicts.

### 3. Set your service name - the only setting you actually need

```yaml
traceflow:
  service:
    name: my-service
```

Tracing, metrics, JSON logging, and Kubernetes metadata are all on by
default. Full property list and env var equivalents in
[docs/configuration.md](docs/configuration.md).

### 4. Expose the actuator endpoints Prometheus needs to scrape

TraceFlow does not override Spring Boot's own (deliberately conservative)
default of exposing only `/actuator/health` over HTTP - add the ones you
want scraped or browsed:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

### 5. Point traces at your OTel Collector

```yaml
traceflow:
  otlp:
    endpoint: http://traceflow-collector:4318
```

Optional for local development: if you skip this, spans export to
`http://localhost:4318/v1/traces` and silently drop if nothing's listening
there - your app is never blocked or slowed by a missing collector
(Section 15's failure-isolation requirement).

### Verify it worked

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus | head
```

Make a request against any of your own endpoints, then check the console:
a JSON log line with `traceId`/`spanId` fields should appear for any log
statement executed while handling that request.

### What you get, with zero other code changes

- JVM + HTTP metrics at `/actuator/prometheus`
- Distributed traces over OTLP/HTTP, with the `traceparent` header
  propagated automatically on outbound `RestClient`/`RestTemplate`/
  `WebClient` calls - no manual header code (see `examples/order-service`
  for a working two-service proof)
- Structured JSON logs on stdout with automatic trace correlation and
  sensitive-field masking (`password`, `token`, `authorization`, etc.)
- Kubernetes pod identity (`k8s.pod.name`, `k8s.namespace.name`, ...)
  attached to every trace automatically when running in a cluster
- A health indicator that reports only TraceFlow's own state - never
  affected by an unreachable collector, Prometheus, or Grafana

For the full walkthrough - including running the bundled Docker Compose
stack so you have somewhere to actually see this data - see
[docs/getting-started.md](docs/getting-started.md).

## Architecture

```text
                    Kubernetes / OpenShift
┌──────────────────────────────────────────────────────────────┐
│   Spring Boot App                                             │
│   ┌───────────────────────┐                                   │
│   │ TraceFlow Starter     │                                   │
│   │  Metrics / Tracing /  │                                   │
│   │  Logging / Correlation│                                   │
│   └──────────┬────────────┘                                   │
│         OTLP │      \___________________                      │
│              ▼                          │ /actuator/prometheus │
│   ┌────────────────────────┐            │                     │
│   │ OpenTelemetry Collector │           │                     │
│   └───────────┬─────────────┘           │                     │
│               ▼                        ▼                     │
│            Tempo                  Prometheus  <── stdout ──┐  │
│               │                        │             Promtail│
│               └────────────┬───────────┘                 │  │
│                             ▼                             ▼  │
│                          Grafana  <───────────────────  Loki  │
└──────────────────────────────────────────────────────────────┘
```

Metrics are scraped directly from each app's `/actuator/prometheus` (the
standard Micrometer pattern - no translation layer needed). Traces go
through the OTel Collector to Tempo. Logs are structured JSON on stdout,
shipped to Loki by Promtail. Full rationale in
[docs/architecture.md](docs/architecture.md); an interactive, explorable
version of this diagram (pan/zoom, per-signal guided views, light/dark) is
at [docs/architecture-diagram.html](docs/architecture-diagram.html).

## Quick Start

```bash
git clone <repo>
cd traceflow-lite
mvn clean verify
docker compose up -d --build
```

Then open:

- Demo app: http://localhost:8080/api/products
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / admin)

See [docs/getting-started.md](docs/getting-started.md) for a walkthrough of
the demo endpoints and what to look at in Grafana.

## Kubernetes

```bash
kubectl apply -f deployment/kubernetes/
```

Or with Helm:

```bash
helm install traceflow deployment/helm/traceflow-lite
```

See [docs/kubernetes.md](docs/kubernetes.md).

## OpenShift

The same manifests work on OpenShift without modification - no privileged
containers, no hardcoded UIDs, no host networking. See
[docs/openshift.md](docs/openshift.md) for the specifics of why.

## Configuration

The full property reference is in
[docs/configuration.md](docs/configuration.md). The short version:

```yaml
traceflow:
  enabled: true
  service:
    name: ${SERVICE_NAME:unknown-service}
  environment:
    name: ${ENVIRONMENT:local}
  tracing:
    enabled: true
    sampling-probability: 1.0
  metrics:
    enabled: true
  logging:
    enabled: true
    json-enabled: true
  otlp:
    endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

## Dashboards

Three Grafana dashboards are provisioned automatically: Application
Overview, Kubernetes Application, and Distributed Tracing. See
[docs/dashboards.md](docs/dashboards.md).

## Modules

| Module | Purpose |
|---|---|
| `traceflow-core` | Framework-agnostic correlation context, config model, Kubernetes metadata. No Spring dependency. |
| `traceflow-spring-boot-autoconfigure` | Property binding, health/info contributors, JSON logging + masking, and the property bridge onto Spring Boot Actuator's own OpenTelemetry auto-configuration. |
| `traceflow-spring-boot-starter` | The one dependency an application adds. |
| `traceflow-demo` | Reference app exercising every signal and failure scenario. |
| `examples/order-service`, `examples/payment-service` | Two-service distributed trace demo. |
| `examples/benchmark` | TraceFlow-enabled vs disabled measurement script. |

## Test Results

`mvn clean install` from the repo root builds and tests all four core
modules. As of this writing: 29 tests across `traceflow-core` (9),
`traceflow-spring-boot-autoconfigure` (11), and `traceflow-demo` (9,
including a dedicated collector-unreachable failure-isolation test) - all
green. The two-service distributed demo was run manually and confirmed the
same `traceId` appears in both `order-service` and `payment-service` logs
for a single request chain.

### Performance results

Measured with `examples/benchmark/run-benchmark.sh` on this development
machine (Windows, single run - see the script for methodology and caveats):

| | TraceFlow enabled | TraceFlow disabled |
|---|---|---|
| Startup time (Spring Boot's own figure) | 12.49s | 14.47s |
| Avg latency, 50 sequential `/api/products` requests | 39ms | 44ms |

TraceFlow enabled was not slower than disabled in this run - the
difference is within normal run-to-run noise (JIT warmup, OS scheduling),
and is small relative to the ~12-14s that Spring Data JPA + Hibernate +
Tomcat startup already costs regardless of TraceFlow. Re-run the script on
your own hardware before making any capacity-planning decision from these
numbers; Section 23 explicitly asks not to generalize a single machine's
measurement.

## Known Limitations

- Docker Compose stack (Prometheus/Loki/Tempo/Grafana/Collector) is written
  and JSON/YAML-validated but not yet run end-to-end in this environment
  (no local Docker daemon available at build time) - see
  [docs/troubleshooting.md](docs/troubleshooting.md) if something doesn't
  come up cleanly.
- The "Kubernetes Application" Grafana dashboard needs kube-state-metrics +
  cAdvisor in the cluster's Prometheus; it stays empty against the local
  compose stack by design.
- Tempo's service graph is not wired up by default (needs the
  metrics-generator + Prometheus remote-write) - documented as a follow-up
  in the Distributed Tracing dashboard itself and in
  [docs/tracing.md](docs/tracing.md).
- JDBC calls are not automatically instrumented as separate spans (would
  require a third-party datasource-proxy dependency outside this project's
  chosen stack); they're covered by the parent HTTP span's duration.

## Roadmap

- Phase 2 (design, not implemented): a `traceflow` CLI (`init`, `validate`,
  `status`, `dashboard`).
- Future phase (design, not implemented): a TraceFlow AI Analyzer extension
  point correlating metrics/logs/traces into incident context for an LLM to
  reason over. The MVP stays deterministic and infrastructure-focused.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0 - see [LICENSE](LICENSE).
