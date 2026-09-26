package io.traceflow.demo.config;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Captures the actual bound HTTP port once the embedded server starts.
 *
 * <p>{@code server.port} in the Environment stays "0" for random-port runs
 * (tests, or {@code SERVER_PORT=0} in production) - the real port is only
 * known after {@link WebServerInitializedEvent} fires, which happens during
 * context refresh, before any request can arrive. Consumers that need to call
 * the app's own port (like {@code OrderService}'s downstream call) must read
 * it lazily through this holder rather than via {@code @Value} at construction time.
 */
@Component
public class ServerPortHolder implements ApplicationListener<WebServerInitializedEvent> {

    private volatile int port = -1;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    public int port() {
        if (port < 0) {
            throw new IllegalStateException("Server port requested before the web server started");
        }
        return port;
    }
}
