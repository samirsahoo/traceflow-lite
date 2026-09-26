# Contributing to TraceFlow Lite

## Ground rules

- **Don't reinvent OpenTelemetry, Micrometer, or Spring Boot Actuator's own
  observability auto-configuration.** If a signal (metrics/tracing/logging)
  already has a Spring Boot or Micrometer mechanism, bridge onto it in
  `traceflow-spring-boot-autoconfigure` rather than building a parallel one.
  See [docs/architecture.md](docs/architecture.md) for why this project
  already changed direction once on this exact point.
- **`traceflow-core` never gains a Spring dependency.** It stays usable
  outside a Spring Boot application.
- **Never make a health check depend on an observability backend.**
  Grafana/Prometheus/Loki/Tempo/the collector being down must never turn
  `/actuator/health` red (Section 14/15).
- **Never add a metric or log label with unbounded cardinality** (raw
  request IDs, user IDs, emails, resolved URLs). See
  [docs/metrics.md](docs/metrics.md).
- **Keep the number of Grafana dashboards small.** Extend an existing one
  before adding a new one.

## Workflow

1. Fork, branch from `main`.
2. `mvn clean verify` (JDK 21) must pass across all four reactor modules.
3. If you touched `examples/order-service` or `examples/payment-service`,
   build and run them manually - they're intentionally not reactor modules
   (see [docs/development.md](docs/development.md)).
4. Conventional-commit messages; one logical change per PR.
5. Update the relevant `docs/*.md` for any behavior or property change.

## Adding a configuration property

See [docs/development.md](docs/development.md#adding-a-new-configuration-property).

## Reporting a security issue

See [SECURITY.md](SECURITY.md).
