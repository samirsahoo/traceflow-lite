#!/usr/bin/env bash
# Section 23 performance benchmark: TraceFlow enabled vs disabled, on the
# already-built traceflow-demo.jar (run `mvn -pl traceflow-demo -am package`
# first). Measures Spring Boot's own reported startup time, the jar's peak
# working-set memory once warmed up, and average latency of 50 sequential
# requests to /api/products.
#
# Methodology, and its limits:
#   - Startup time is Spring Boot's own self-reported figure from the
#     "Started ... in X seconds" log line, not wall-clock around the process
#     - excludes JVM/OS process launch overhead, which is identical in both
#       runs and irrelevant to what TraceFlow itself costs.
#   - Memory is a single working-set snapshot 3s after the app reports ready,
#     not peak-over-time; treat it as directional, not a hard ceiling.
#   - Latency is 50 *sequential* GET requests (no concurrency), so it
#     measures per-request overhead, not throughput under load.
#   - Numbers vary by machine. Do not copy these into marketing claims -
#     Section 23 explicitly asks for measured numbers, re-measured on
#     whatever hardware you actually care about.
set -euo pipefail

JAR="$(dirname "$0")/../../traceflow-demo/target/traceflow-demo.jar"
PORT=8099
REQUESTS=50

run_variant() {
  local label="$1" extra_args="$2"
  echo "=== $label ==="

  java -jar "$JAR" --server.port=$PORT $extra_args > "/tmp/traceflow-bench-$label.log" 2>&1 &
  local pid=$!

  for _ in $(seq 1 60); do
    curl -s -o /dev/null "http://localhost:$PORT/actuator/health" && break
    sleep 1
  done

  local startup
  startup=$(grep -o "Started TraceFlowDemoApplication in [0-9.]* seconds" "/tmp/traceflow-bench-$label.log" | grep -o "[0-9.]*" | head -1)
  echo "Startup time: ${startup}s"

  sleep 3
  echo "Working set: $(ps -o rss= -p "$pid" 2>/dev/null || echo "n/a") KB"

  local start end
  start=$(date +%s%N)
  for _ in $(seq 1 $REQUESTS); do
    curl -s -o /dev/null "http://localhost:$PORT/api/products"
  done
  end=$(date +%s%N)
  echo "Avg latency over $REQUESTS sequential requests: $(( (end - start) / REQUESTS / 1000000 ))ms"

  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  echo
}

run_variant "traceflow-enabled"  ""
run_variant "traceflow-disabled" "--traceflow.enabled=false"
