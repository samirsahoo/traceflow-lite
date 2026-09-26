package io.traceflow.demo.web.dto;

import io.traceflow.demo.domain.Order;

import java.time.Instant;

public record OrderResponse(Long id, String product, int quantity, String status, Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getProduct(), order.getQuantity(),
                order.getStatus(), order.getCreatedAt());
    }
}
