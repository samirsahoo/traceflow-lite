package io.traceflow.examples.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Client -&gt; order-service -&gt; payment-service (Section 21). The
 * {@code traceparent} header on the outbound call to payment-service is
 * added automatically by Spring Boot's RestClient observation instrumentation
 * once TraceFlow's tracing bridge is active - no code here does it manually.
 */
@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestClient paymentServiceClient;

    public OrderController(RestClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
    }

    @PostMapping("/api/orders/{orderId}/checkout")
    public Map<String, Object> checkout(@PathVariable String orderId) {
        log.info("Checking out order {}, calling payment-service", orderId);

        Map<?, ?> payment = paymentServiceClient.post()
                .uri("/api/payments")
                .body(Map.of("orderId", orderId, "amount", 42.00))
                .retrieve()
                .body(Map.class);

        log.info("Order {} checkout complete: {}", orderId, payment);
        return Map.of("orderId", orderId, "payment", payment, "checkoutId", UUID.randomUUID().toString());
    }
}
