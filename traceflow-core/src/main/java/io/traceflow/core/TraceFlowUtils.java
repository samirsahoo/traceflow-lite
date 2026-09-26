package io.traceflow.core;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Stateless helpers used by both the core and autoconfigure modules: Kubernetes
 * metadata detection, correlation id generation, and sensitive-value masking.
 * Kept as static methods (no framework abstractions) per Section 5's instruction
 * not to build unnecessary wrappers.
 */
public final class TraceFlowUtils {

    /**
     * Header/config-key names that must never appear in logs or span attributes
     * in cleartext (Section 24). Matched case-insensitively against a normalized
     * (lower-cased, non-alphanumeric-stripped) key, so "Authorization",
     * "authorization-header" and "AUTH_TOKEN" all match.
     */
    private static final Set<String> DEFAULT_SENSITIVE_KEY_FRAGMENTS = Set.of(
            "password", "passwd", "secret", "token", "authorization", "apikey",
            "api_key", "creditcard", "credit_card", "ccnumber", "cvv", "jwt", "cookie"
    );

    private static final String MASK = "***";

    private TraceFlowUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    /** Short, dependency-free correlation id for requests with no active span. */
    public static String generateCorrelationId() {
        return Long.toHexString(System.nanoTime()) + Integer.toHexString(
                (int) (Math.random() * Integer.MAX_VALUE));
    }

    /**
     * Detects Kubernetes/OpenShift pod identity from environment variables.
     * {@code envLookup} is injected (rather than calling {@link System#getenv()}
     * directly) so tests can simulate both in-cluster and local execution.
     */
    public static KubernetesMetadata detectKubernetesMetadata(Function<String, String> envLookup) {
        boolean inCluster = !isBlank(envLookup.apply(TraceFlowConstants.ENV_KUBERNETES_SERVICE_HOST));
        if (!inCluster) {
            return KubernetesMetadata.notInCluster();
        }
        return new KubernetesMetadata(
                true,
                defaultIfBlank(envLookup.apply(TraceFlowConstants.ENV_POD_NAME), ""),
                defaultIfBlank(envLookup.apply(TraceFlowConstants.ENV_POD_NAMESPACE), ""),
                defaultIfBlank(envLookup.apply(TraceFlowConstants.ENV_POD_IP), ""),
                defaultIfBlank(envLookup.apply(TraceFlowConstants.ENV_NODE_NAME), ""),
                defaultIfBlank(envLookup.apply(TraceFlowConstants.ENV_CONTAINER_NAME), "")
        );
    }

    public static KubernetesMetadata detectKubernetesMetadata() {
        return detectKubernetesMetadata(System::getenv);
    }

    public static boolean isSensitiveKey(String key) {
        return isSensitiveKey(key, DEFAULT_SENSITIVE_KEY_FRAGMENTS);
    }

    public static boolean isSensitiveKey(String key, Set<String> sensitiveFragments) {
        if (isBlank(key)) {
            return false;
        }
        String normalized = key.toLowerCase().replaceAll("[^a-z0-9]", "");
        for (String fragment : sensitiveFragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    /** Masks a single value, always returning a fixed-width marker (never the length-preserving kind, to avoid leaking value length as a side channel). */
    public static String mask(String value) {
        return value == null ? null : MASK;
    }

    /**
     * Applies masking to a key/value map (e.g. HTTP headers) using the default
     * sensitive-key list. Returns a new map; the input is never mutated.
     */
    public static Map<String, String> maskSensitiveEntries(Map<String, String> entries) {
        return maskSensitiveEntries(entries, DEFAULT_SENSITIVE_KEY_FRAGMENTS, TraceFlowUtils::mask);
    }

    public static Map<String, String> maskSensitiveEntries(
            Map<String, String> entries, Set<String> sensitiveFragments, UnaryOperator<String> masker) {
        return entries.entrySet().stream().collect(
                java.util.LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(),
                        isSensitiveKey(entry.getKey(), sensitiveFragments) ? masker.apply(entry.getValue()) : entry.getValue()),
                java.util.LinkedHashMap::putAll);
    }
}
