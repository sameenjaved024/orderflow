package com.orderflow.fulfillment.service;

import com.orderflow.fulfillment.config.RabbitMQConfig;
import com.orderflow.fulfillment.domain.Fulfillment;
import com.orderflow.fulfillment.domain.FulfillmentStatus;
import com.orderflow.fulfillment.event.FulfillmentCompletedEvent;
import com.orderflow.fulfillment.event.OrderCreatedEvent;
import com.orderflow.fulfillment.repository.FulfillmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FulfillmentService {

    private final FulfillmentRepository fulfillmentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void processOrder(OrderCreatedEvent event) {
        log.info("Processing order: orderId={}, customerId={}",
                event.orderId(), event.customerId());

        // Idempotency — skip if already processed
        if (fulfillmentRepository.existsByOrderId(event.orderId())) {
            log.warn("Duplicate event for orderId={} — skipping", event.orderId());
            return;
        }

        // Step 1 — Create fulfillment record
        Fulfillment fulfillment = Fulfillment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .status(FulfillmentStatus.RECEIVED)
                .build();

        fulfillmentRepository.save(fulfillment);
        log.info("Fulfillment created: fulfillmentId={}", fulfillment.getId());

        // Step 2 — Simulate warehouse processing
        fulfillment.setStatus(FulfillmentStatus.PROCESSING);
        fulfillment.setStatus(FulfillmentStatus.PACKED);
        fulfillment.markDispatched();

        // Step 3 — Save final DISPATCHED status
        fulfillmentRepository.save(fulfillment);
        log.info("Fulfillment dispatched: fulfillmentId={}", fulfillment.getId());

        // Step 4 — Publish to RabbitMQ after DB write
        publishFulfillmentCompletedEvent(fulfillment);

    }

    private void publishFulfillmentCompletedEvent(Fulfillment fulfillment) {
        FulfillmentCompletedEvent event = FulfillmentCompletedEvent.of(
                fulfillment.getId(),
                fulfillment.getOrderId(),
                fulfillment.getCustomerId(),
                fulfillment.getStatus()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );

        log.info("FulfillmentCompletedEvent published: orderId={}",
                fulfillment.getOrderId());
    }
}
