# Logging

## Format

Structured JSON on stdout by default:

```json
{
  "@timestamp": "2026-09-26T18:52:44.66Z",
  "message": "Validated product before order creation: Widget",
  "logger_name": "io.traceflow.demo.service.OrderService",
  "level": "INFO",
  "traceId": "1771a419fd8f4d0f6d6ee51262767173",
  "spanId": "39b2b2274be62253",
  "service": "traceflow-demo",
  "environment": "local"
}
```

`traceId`/`spanId` appear automatically whenever a request is inside an
active span - no manual MDC code. `service`/`environment` are static custom
fields set once from the resolved TraceFlow configuration.

## How it's wired

`TraceFlowLoggingConfiguration` reconfigures the root Logback logger's
appender programmatically (a `ConsoleAppender` with a `LogstashEncoder`)
during Spring context startup, rather than shipping a `logback-spring.xml`
inside the starter jar. The reason: a library-provided `logback-spring.xml`
on the classpath would silently compete with the consuming application's
own file, with no clear precedence rule. The one trade-off: log lines
emitted before the Spring context refreshes (very early startup) still use
Spring Boot's default console pattern, not JSON.

To disable JSON and keep the default pattern:

```yaml
traceflow:
  logging:
    json-enabled: true   # set to false
```

## Masking sensitive fields (Section 9/24)

A `MaskingJsonGeneratorDecorator` masks these field names to `***`
wherever they appear in the JSON output, by default:

```text
password, passwd, secret, token, authorization, apiKey, api_key,
creditCard, credit_card, cvv, jwt, cookie
```

This is a field-name match on the JSON structure being emitted, not a
regex over free-text log messages - if application code logs a sensitive
value as part of a plain string message (`log.info("token=" + token)`)
rather than as a structured field, masking won't catch it. Prefer
structured logging (`log.info("token={}", StructuredArguments.kv("token",
token))` or similar) for anything sensitive, so the masking rule applies.

`TraceFlowUtils.maskSensitiveEntries()` (in `traceflow-core`) applies the
same field-name matching to any `Map<String,String>` - useful if your own
code wants to log a headers map safely.

## Correlation without an active span

`TraceFlowContext.currentCorrelationId()` falls back to a per-thread
generated ID when there's no active span (e.g. very early request filters,
scheduled jobs with tracing disabled), so log correlation still works in
code paths that never touch the tracing bridge.
