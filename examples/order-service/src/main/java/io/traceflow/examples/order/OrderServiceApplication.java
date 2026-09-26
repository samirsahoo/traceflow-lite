package io.traceflow.examples.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Bean
    RestClient paymentServiceClient(RestClient.Builder builder, @Value("${payment-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
