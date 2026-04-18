package com.orderflow.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
