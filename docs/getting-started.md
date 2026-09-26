# Getting Started

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (for the full local stack; the demo app alone needs nothing else)

## Build and run the demo app alone

```bash
cd traceflow-lite
mvn -pl traceflow-demo -am clean package
java -jar traceflow-demo/target/traceflow-demo.jar
```

It starts on port 8080 with an in-memory H2 database and no observability
backend required - traces are exported best-effort to
`http://localhost:4318/v1/traces` and simply dropped if nothing is
listening there (Section 15).

Try each scenario:

```bash
curl http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"product":"widget","quantity":2}'
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/slow
curl -i http://localhost:8080/api/failure
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus | head -20
```

Watch the console: every request logs a JSON line with a `traceId`/`spanId`
that's shared across the whole request (including the internal downstream
call `/api/orders` makes to `/api/products` to validate the product).

## Bring up the full stack

```bash
mvn -pl traceflow-demo -am clean package
docker compose up -d --build
```

Then:

1. Hit a few endpoints (as above) against `http://localhost:8080`.
2. Open Prometheus at http://localhost:9090 and query
   `http_server_requests_seconds_count`.
3. Open Grafana at http://localhost:3000 (admin/admin) - the "TraceFlow
   Lite" folder has all three dashboards, already pointed at Prometheus,
   Loki, and Tempo with no setup.
4. On the Distributed Tracing dashboard, the "Recent Traces" panel shows
   the actual spans your curl commands generated.

## Run the two-service distributed demo

```bash
mvn -pl traceflow-demo -am clean package   # builds traceflow-spring-boot-starter too
cd examples/payment-service && mvn clean package && java -jar target/payment-service.jar &
cd ../order-service && mvn clean package && java -jar target/order-service.jar &

curl -X POST http://localhost:8081/api/orders/ORDER-123/checkout
```

Check both services' logs for the request: the `traceId` field is identical
in `order-service`'s "Checking out order..." line and `payment-service`'s
"Processing payment..." line, even though they're two separate JVM
processes - the `traceparent` header did its job.

## Run the benchmark

```bash
mvn -pl traceflow-demo -am clean package
bash examples/benchmark/run-benchmark.sh
```

See [performance results in the README](../README.md#test-results) and the
script's own header comment for methodology and its limits.
