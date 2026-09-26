{{- define "traceflow-lite.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "traceflow-lite.fullname" -}}
{{- .Release.Name -}}
{{- end -}}

{{- define "traceflow-lite.labels" -}}
app.kubernetes.io/part-of: {{ include "traceflow-lite.name" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}
