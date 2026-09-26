# Security Policy

## Reporting a vulnerability

Please do not open a public GitHub issue for a suspected security
vulnerability. Instead, email `security@traceflow.example` (placeholder)
with a description and, if possible, a reproduction. We aim to acknowledge
within 3 business days.

## What this project does to reduce risk (Section 24)

- **Never logs secrets in cleartext.** `TraceFlowLoggingConfiguration`
  masks `password`, `token`, `authorization`, `apiKey`, `creditCard`, `jwt`,
  `cookie`, and related field names in every JSON log line by default. See
  [docs/logging.md](docs/logging.md) for its limits (field-name matching,
  not free-text scanning).
- **Non-root containers.** `deployment/docker/Dockerfile` never runs as
  root; Kubernetes/Helm manifests set `runAsNonRoot`,
  `allowPrivilegeEscalation: false`, and drop all Linux capabilities. See
  [docs/openshift.md](docs/openshift.md) for the full rationale.
- **Pinned dependency versions**, centrally managed in the root `pom.xml`'s
  `dependencyManagement` - no module hardcodes its own version.
- **Automated dependency scanning and SBOM generation** in CI
  (`.github/workflows/security.yml`): OWASP `dependency-check` and a
  CycloneDX SBOM on every push/PR plus a weekly scheduled run.
- **Container image scanning** in CI (`.github/workflows/docker.yml`) via
  Trivy before any image would be published (images are never pushed
  automatically - Section 25).

## Scope

This is an observability library and reference application, not a security
product. It does not perform authentication, authorization, or encryption
of telemetry data in transit beyond what the underlying OTLP/Prometheus/
Loki transport already provides - configure TLS on those endpoints
yourself for anything beyond local development.
