# OpenShift Compatibility

The manifests in `deployment/kubernetes/` and the Helm chart in
`deployment/helm/traceflow-lite/` work on OpenShift without modification.
This document explains why, so nothing here relies on luck.

## What OpenShift's default Security Context Constraint (`restricted-v2`) forbids

- Running as root, or as any specific hardcoded UID the pod spec asks for.
- Privileged containers.
- Host networking, host PID, host IPC.
- Most Linux capabilities (only a small default set is allowed).

## What TraceFlow Lite does about each

| Requirement | How it's satisfied |
|---|---|
| No root | `deployment/docker/Dockerfile` creates a dedicated `traceflow` user and sets `USER traceflow`. `securityContext.runAsNonRoot: true` is also set at the pod level as a defense-in-depth check. |
| No hardcoded UID | **Nothing sets `runAsUser`.** This is deliberate: OpenShift's SCC assigns an arbitrary UID from the namespace's allocated range at admission time. A pod spec that hardcodes `runAsUser: 1000` (a common mistake) would be rejected by `restricted-v2` unless that exact UID happens to be in the allowed range. Leaving it unset lets OpenShift do the assignment; the container image still refuses to run as root (it has no `root`-owned entrypoint requirement). |
| No privileged escalation | `allowPrivilegeEscalation: false` and `capabilities.drop: ["ALL"]` on every container. |
| No host networking/PID | Never set - the manifests use standard `ClusterIP` Services and Deployments throughout. |
| Seccomp | `seccompProfile.type: RuntimeDefault` at the pod level. |

## Deploying

```bash
oc new-project traceflow-lite
oc apply -f deployment/kubernetes/ -n traceflow-lite
```

Or with Helm (`oc` clusters have Helm 3 support with no extra setup):

```bash
helm install traceflow deployment/helm/traceflow-lite -n traceflow-lite --create-namespace
```

## Routes

The manifests create Kubernetes `Service` objects, not OpenShift `Route`
objects (to keep the base manifests portable to plain Kubernetes too).
Expose the demo app externally with:

```bash
oc expose service traceflow-demo -n traceflow-lite
oc get route traceflow-demo -n traceflow-lite
```

## Verifying SCC compliance yourself

```bash
oc get pod -n traceflow-lite -o jsonpath='{.items[0].spec.securityContext}'
oc get pod -n traceflow-lite -o jsonpath='{.items[0].spec.containers[0].securityContext}'
```

Confirm no `runAsUser` is present and `runAsNonRoot: true` /
`allowPrivilegeEscalation: false` are.
