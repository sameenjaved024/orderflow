package com.orderflow.fulfillment.event;

import com.orderflow.fulfillment.domain.FulfillmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound event published to RabbitMQ when fulfillment is completed.
 */
public record FulfillmentCompletedEvent(
        UUID fulfillmentId,
        UUID orderId,
        String customerId,
        FulfillmentStatus status,
        Instant occurredAt
) {
    public static FulfillmentCompletedEvent of(
            UUID fulfillmentId,
            UUID orderId,
            String customerId,
            FulfillmentStatus status) {
        return new FulfillmentCompletedEvent(
                fulfillmentId,
                orderId,
                customerId,
                status,
                Instant.now()
        );
    }
}
