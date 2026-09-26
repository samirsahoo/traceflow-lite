# Development

## Build

```bash
cd traceflow-lite
mvn clean install
```

Builds and tests all four modules in dependency order:
`traceflow-core` → `traceflow-spring-boot-autoconfigure` →
`traceflow-spring-boot-starter` → `traceflow-demo`.

`examples/order-service`, `examples/payment-service`, and
`examples/benchmark` are intentionally **not** reactor modules of the root
`pom.xml` - they're meant to be run as standalone reference apps, each with
its own `spring-boot-starter-parent`, depending on
`io.traceflow:traceflow-spring-boot-starter:0.1.0-SNAPSHOT` from your local
Maven repository (installed by the root build above). Build them
individually:

```bash
cd examples/payment-service && mvn clean package
cd examples/order-service && mvn clean package
```

## Project layout

See the module table in the root [README.md](../README.md#modules).

## Adding a new configuration property

1. Add the field to the relevant nested class in `TraceFlowProperties`
   (`traceflow-spring-boot-autoconfigure`).
2. If it needs to influence *other* auto-configuration's conditions or
   `@ConfigurationProperties` binding (anything under `management.*`), add
   the translation to `TraceFlowEnvironmentPostProcessor` instead of a
   `@Bean` - by the time `@Bean` methods run, other auto-configuration
   classes have already evaluated their conditions.
3. Document it in [docs/configuration.md](configuration.md).
4. Add a test - either an `ApplicationContextRunner` test (autoconfigure
   module, for anything that doesn't depend on `EnvironmentPostProcessor`
   timing) or a `MockEnvironment`-based unit test on the post-processor
   directly (see `TraceFlowEnvironmentPostProcessorTest` for the pattern).

## Testing conventions

- `traceflow-core`: plain JUnit 5 + AssertJ, no Spring.
- `traceflow-spring-boot-autoconfigure`: `ApplicationContextRunner` for
  bean-wiring assertions; direct unit tests for anything that needs an
  `EnvironmentPostProcessor` (which `ApplicationContextRunner` never
  invokes, since it doesn't go through `SpringApplication.run()`).
- `traceflow-demo`: `@SpringBootTest(webEnvironment = RANDOM_PORT)` +
  `TestRestTemplate` for integration behavior. Add
  `@AutoConfigureObservability` to any test that needs the *real* metrics/
  tracing backends active - Spring Boot disables them by default under
  `@SpringBootTest` to keep unrelated tests fast, which will silently break
  an assertion like "the Prometheus endpoint returns data" if forgotten.

## Releasing

Not yet defined for this MVP - `version` is `0.1.0-SNAPSHOT` throughout.
