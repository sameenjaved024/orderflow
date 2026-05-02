package com.orderflow.fulfillment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event consumed from Kafka.
 * Mirrors OrderCreatedEvent published by order-service.
 */
public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount,
        Instant occurredAT
) { }
