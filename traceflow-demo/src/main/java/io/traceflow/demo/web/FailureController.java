package io.traceflow.demo.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Deliberate failure/latency scenarios (Section 20) for demonstrating error
 * traces, error rate dashboards, and the P95/P99 latency panels.
 */
@RestController
public class FailureController {

    private static final Logger log = LoggerFactory.getLogger(FailureController.class);

    @GetMapping("/api/failure")
    public String fail() {
        log.error("Simulated failure endpoint invoked - about to throw");
        throw new IllegalStateException("Simulated downstream failure");
    }

    @GetMapping("/api/slow")
    public String slow() throws InterruptedException {
        int delayMs = ThreadLocalRandom.current().nextInt(500, 3000);
        log.info("Simulated slow endpoint invoked, sleeping {}ms", delayMs);
        Thread.sleep(delayMs);
        return "Completed after " + delayMs + "ms";
    }
}
