# Configuration Reference

## `traceflow.*` properties

| Property | Default | Description |
|---|---|---|
| `traceflow.enabled` | `true` | Master switch. `false` disables every TraceFlow bean; the app still starts and runs normally. |
| `traceflow.service.name` | `unknown-service` | Resolution order: this property > `OTEL_SERVICE_NAME` > `spring.application.name` > the default. |
| `traceflow.environment.name` | `local` | Free-form deployment environment label (`prod`, `staging`, ...). |
| `traceflow.tracing.enabled` | `true` | Independently disables tracing while leaving metrics/logging on. |
| `traceflow.tracing.sampling-probability` | `1.0` | Forwarded to `management.tracing.sampling.probability`. |
| `traceflow.metrics.enabled` | `true` | Independently disables the Prometheus metrics export. |
| `traceflow.logging.enabled` | `true` | Independently disables TraceFlow's logging configuration entirely. |
| `traceflow.logging.json-enabled` | `true` | When `false`, keeps Spring Boot's default console pattern instead of switching to JSON. |
| `traceflow.otlp.endpoint` | `http://localhost:4318/v1/traces` | Resolution order: this property > `OTEL_EXPORTER_OTLP_ENDPOINT` > the default. A bare `host:port` (no path) is accepted and normalized automatically. |

## Environment variables (Section 16)

Every `traceflow.*` property above is also settable as an environment
variable via Spring Boot's standard relaxed binding -
`TRACEFLOW_SERVICE_NAME`, `TRACEFLOW_TRACING_ENABLED`,
`TRACEFLOW_METRICS_ENABLED`, `TRACEFLOW_LOGGING_ENABLED`, and so on. No
extra code is needed for this; it's Spring Boot's own environment variable
→ property name mapping.

Two OpenTelemetry-native environment variables are honored as a secondary
fallback, specifically so an app that already sets these for other tooling
doesn't need TraceFlow-specific duplicates:

- `OTEL_SERVICE_NAME`
- `OTEL_EXPORTER_OTLP_ENDPOINT`

Precedence for both service name and OTLP endpoint is: explicit
`traceflow.*` property (from `application.yml`, `TRACEFLOW_*` env var, or a
system property) > the OTel-native env var > TraceFlow's own default.

## What TraceFlow does NOT read directly

Once resolved, TraceFlow writes several native Spring Boot Actuator
properties on your behalf (via `TraceFlowEnvironmentPostProcessor`), at the
lowest property-source precedence - so if your application sets any of
these itself, yours wins:

- `spring.application.name`
- `management.opentelemetry.resource-attributes.*`
- `management.tracing.enabled` / `management.tracing.sampling.probability`
- `management.otlp.tracing.endpoint`
- `management.metrics.export.prometheus.enabled`

If you need to configure the tracing/metrics backend beyond what
`traceflow.*` exposes (custom headers, compression, timeouts), set the
native `management.otlp.tracing.*` properties directly - see
[Spring Boot's Actuator documentation](https://docs.spring.io/spring-boot/reference/actuator/index.html)
for the full list.

## Example: production-like configuration

```yaml
traceflow:
  service:
    name: payment-service
  environment:
    name: prod
  tracing:
    sampling-probability: 0.1   # sample 10% in a high-traffic prod service
  otlp:
    endpoint: http://traceflow-collector.observability.svc:4318
```
