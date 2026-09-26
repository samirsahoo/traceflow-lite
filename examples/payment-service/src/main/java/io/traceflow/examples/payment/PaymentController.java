package io.traceflow.examples.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * The leaf of the distributed trace (Section 21):
 * Client -&gt; order-service -&gt; payment-service.
 * No further downstream call - traceparent propagation into this process is
 * enough to prove the whole chain works.
 */
@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @PostMapping("/api/payments")
    public Map<String, Object> charge(@org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        log.info("Processing payment for order {}", request.get("orderId"));
        return Map.of(
                "paymentId", UUID.randomUUID().toString(),
                "orderId", request.get("orderId"),
                "status", "CHARGED"
        );
    }

}
