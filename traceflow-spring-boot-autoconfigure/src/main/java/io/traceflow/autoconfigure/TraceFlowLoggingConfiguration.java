package io.traceflow.autoconfigure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import io.traceflow.core.TraceFlowConfiguration;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.mask.MaskingJsonGeneratorDecorator;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Section 9: structured JSON logs with automatic traceId/spanId correlation
 * and sensitive-field masking.
 *
 * <p>{@code Slf4JEventListener} (wired in {@link TraceFlowTracingConfiguration})
 * already puts {@code traceId}/{@code spanId} into SLF4J's MDC whenever a span
 * scope is active; {@link LogstashEncoder} includes all MDC entries as
 * top-level JSON fields by default, so no extra correlation code is needed
 * here - only the encoder/appender wiring and the masking rules.
 *
 * <p>This reconfigures the root logger programmatically rather than shipping
 * a {@code logback-spring.xml} inside the jar, because a library-provided
 * {@code logback-spring.xml} on the classpath would silently compete with the
 * consuming application's own file. The one trade-off: log lines emitted
 * before the Spring context refreshes (very early startup) still use Spring
 * Boot's default pattern, not JSON.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({LoggerContext.class, LogstashEncoder.class})
@ConditionalOnProperty(prefix = "traceflow.logging", name = "enabled", matchIfMissing = true)
class TraceFlowLoggingConfiguration {

    @Bean
    InitializingBean traceFlowJsonLoggingInitializer(TraceFlowProperties properties, TraceFlowConfiguration configuration) {
        return () -> {
            if (!properties.getLogging().isJsonEnabled()) {
                return;
            }
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            MaskingJsonGeneratorDecorator maskingDecorator = new MaskingJsonGeneratorDecorator();
            maskingDecorator.setDefaultMask("***");
            for (String field : new String[]{
                    "password", "passwd", "secret", "token", "authorization",
                    "apiKey", "api_key", "creditCard", "credit_card", "cvv", "jwt", "cookie"}) {
                maskingDecorator.addPath(field);
            }
            // MaskingJsonGeneratorDecorator is itself a Logback LifeCycle: without an explicit
            // start(), its internal decorator delegate chain is never initialized and decorate()
            // throws NPE on the first log line.
            maskingDecorator.start();

            LogstashEncoder encoder = new LogstashEncoder();
            encoder.setContext(context);
            encoder.setCustomFields(
                    "{\"service\":\"" + configuration.serviceName() + "\",\"environment\":\"" + configuration.environmentName() + "\"}");
            encoder.setJsonGeneratorDecorator(maskingDecorator);
            encoder.start();

            ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
            appender.setContext(context);
            appender.setName("TRACEFLOW_JSON_CONSOLE");
            appender.setEncoder(encoder);
            appender.start();

            Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            root.detachAndStopAllAppenders();
            root.addAppender(appender);
        };
    }
}
