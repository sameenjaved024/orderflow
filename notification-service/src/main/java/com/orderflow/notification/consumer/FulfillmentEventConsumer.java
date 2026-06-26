package com.orderflow.notification.consumer;

import com.orderflow.notification.event.FulfillmentCompletedEvent;
import com.orderflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for fulfillment completed events.
 * <p>
 * Separation of concerns:
 * This class handles only RabbitMQ transport mechanics.
 * NotificationService handles only business logic.
 * Neither knows about the other's implementation details.
 * <p>
 * Why @RabbitListener here instead of manual ACK:
 * Unlike Kafka where we need manual ACK for at-least-once guarantees,
 * RabbitMQ with Spring AMQP handles redelivery automatically when
 * an exception is thrown. If send() throws, Spring will nack the
 * message and RabbitMQ redelivers it. This is simpler and correct
 * for our notification use case.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FulfillmentEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.fulfillment")

    public void onFulfillmentCompleted(FulfillmentCompletedEvent event) {
        log.info("Received FulfillmentCompletedEvent: orderId={}, status={}",
                event.orderId(), event.status());

        notificationService.notifyAllChannels(event);
    }
}