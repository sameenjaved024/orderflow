package com.orderflow.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event published to Kafka when an order is successfully created.
 * Uses Java record for compile-time immutability — idiomatic Java 21.
 */
public record OrderCreatedEvent(
        UUID orderId,
        String  customerId,
        String  productId,
        Integer quantity,
        BigDecimal totalAmount,
        Instant occurredAt
)  {
    public static OrderCreatedEvent of(
            UUID orderId,
            String customerId,
            String productId,
            Integer quantity,
            BigDecimal totalAmount) {
        return new OrderCreatedEvent(
                orderId,
                customerId,
                productId,
                quantity,
                totalAmount,
                Instant.now()
        );
    }
}
