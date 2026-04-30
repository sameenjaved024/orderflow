package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.event.OrderCreatedEvent;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.mapper.OrderMapper;
import com.orderflow.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    static final String ORDER_CREATED_TOPIC = "order.created";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final OrderMapper orderMapper;

    /**
     * Creates a new order.
     * <p>
     * Design decision: DB write happens BEFORE Kafka publish.
     * If Kafka fails, the order still exists and can be replayed.
     * If DB fails, no phantom event is published.
     */

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customerId={}, productId={}", request.customerId(), request.productId());

        Order order = orderMapper.toEntity(request);
        Order savedOrder = orderRepository.save(order);

        log.info("Order persisted successfully: orderId={}", savedOrder.getId());

        publishOrderCreatedEvent(savedOrder);

        return orderMapper.toResponse(savedOrder);
    }

    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.of(order.getId(), order.getCustomerId(), order.getProductId(),
                order.getQuantity(), order.getTotalAmount());

        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId().toString(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderCreatedEvent for orderId={}: {}",
                                order.getId(), throwable.getMessage(), throwable);
                    } else {
                        log.info("OrderCreatedEvent published: orderId={}, partition={}, offset={}",
                                order.getId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

    public OrderResponse findOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }
}
