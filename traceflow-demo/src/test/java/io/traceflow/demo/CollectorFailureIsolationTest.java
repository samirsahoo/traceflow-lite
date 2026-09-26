package io.traceflow.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Section 15/22: an unreachable OTLP collector must never break the
 * application. Points the exporter at a closed local port (nothing listens
 * there, so every export attempt fails fast) and asserts the app still
 * starts, serves requests, and reports healthy - the span export failure is
 * silently absorbed by BatchSpanProcessor's async queue, off the request
 * thread, exactly as Section 15 requires.
 *
 * <p>{@code @AutoConfigureObservability} is required here specifically
 * because this test needs the real tracing backend active (so there is an
 * actual export attempt to fail) - Spring Boot disables it by default under
 * {@code @SpringBootTest} to keep unrelated tests fast.
 */
@AutoConfigureObservability
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=1.0")
class CollectorFailureIsolationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void unreachableCollector(DynamicPropertyRegistry registry) {
        // Port 1 is a privileged, never-listening port on every platform this
        // test runs on - connection attempts fail immediately instead of
        // timing out, keeping the test fast.
        registry.add("management.otlp.tracing.endpoint", () -> "http://localhost:1/v1/traces");
    }

    @Test
    void applicationStartsAndServesRequestsWithAnUnreachableCollector() {
        ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");

        ResponseEntity<String> products = restTemplate.getForEntity("/api/products", String.class);
        assertThat(products.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requestsThatCreateSpansStillCompleteQuickly() {
        long start = System.nanoTime();
        ResponseEntity<String> response = restTemplate.getForEntity("/api/products", String.class);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Generous bound: this only guards against the request thread blocking
        // on the (unreachable) export call, not general performance.
        assertThat(elapsedMillis).isLessThan(5000);
    }
}
