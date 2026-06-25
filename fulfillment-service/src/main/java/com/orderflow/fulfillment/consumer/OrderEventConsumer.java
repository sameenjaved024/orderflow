package com.orderflow.fulfillment.consumer;

import com.orderflow.fulfillment.event.OrderCreatedEvent;
import com.orderflow.fulfillment.service.FulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    private final FulfillmentService fulfillmentService;

    @KafkaListener(
            topics = "order.created",
            groupId = "fulfillment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received OrderCreatedEvent: orderId={}, partition={}, offset={}",
                event.orderId(), partition, offset);

        try {
            fulfillmentService.processOrder(event);
            acknowledgment.acknowledge();
            log.info("Event acknowledged: orderId={}", event.orderId());

        } catch (Exception ex) {
            log.error("Failed to process event for orderId={} — not acknowledging",
                    event.orderId(), ex);
        }
    }
}
