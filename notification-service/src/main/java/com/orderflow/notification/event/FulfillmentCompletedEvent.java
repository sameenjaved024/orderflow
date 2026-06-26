package com.orderflow.notification.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event received from RabbitMQ.
 * Mirrors FulfillmentCompletedEvent published by fulfillment-service.
 */
public record FulfillmentCompletedEvent(
        UUID fulfillmentId,
        UUID orderId,
        String customerId,
        String status,
        Instant occurredAt
) {
}