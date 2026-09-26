package io.traceflow.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// Spring Boot disables real metrics/tracing backends under @SpringBootTest by default
// (to keep unrelated tests fast); this test specifically asserts the Prometheus endpoint,
// so it needs the real backend switched back on.
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TraceFlowDemoApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void listsProducts() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/products", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("widget");
    }

    @Test
    void createsAnOrderAfterValidatingTheProductDownstream() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders", java.util.Map.of("product", "widget", "quantity", 2), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"status\":\"CREATED\"");
    }

    @Test
    void rejectsOrdersForUnknownProducts() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders", java.util.Map.of("product", "does-not-exist", "quantity", 1), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void failureEndpointReturns500() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/failure", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void healthEndpointIsUpRegardlessOfObservabilityBackends() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void prometheusEndpointExposesHttpServerRequestsMetric() {
        restTemplate.getForEntity("/api/products", String.class);
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("http_server_requests_seconds_count");
    }
}
