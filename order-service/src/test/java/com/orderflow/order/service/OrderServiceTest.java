package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.event.OrderCreatedEvent;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.mapper.OrderMapper;
import com.orderflow.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    @Mock
    private OrderMapper orderMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, kafkaTemplate, orderMapper);
    }

    // ── Test classes go here ─────────────────────────────────────
    @Nested
    @DisplayName("createOrder")
    class CreateOrder {
        @Test
        @DisplayName("should persist order and publish Kafka event on success")
        void shouldPersistOrderAndPublishEvent() {
            // GIVEN
            CreateOrderRequest request = buildRequest();
            Order mappedOrder = buildOrder();
            Order savedOrder = buildSavedOrder();
            OrderResponse expected = buildOrderResponse(savedOrder);

            given(orderMapper.toEntity(request)).willReturn(mappedOrder);
            given(orderRepository.save(mappedOrder)).willReturn(savedOrder);
            given(orderMapper.toResponse(savedOrder)).willReturn(expected);
            given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(CompletableFuture.completedFuture(null));

            // WHEN
            OrderResponse result = orderService.createOrder(request);

            // THEN
            assertThat(result).isEqualTo(expected);
            then(kafkaTemplate).should()
                    .send(eq("order.created"), anyString(), any(OrderCreatedEvent.class));
        }
    }


        // ── Builder helpers ──────────────────────────────────────────

        private CreateOrderRequest buildRequest() {
            return new CreateOrderRequest(
                    "cust-001", "prod-001", 2, new BigDecimal("49.99"));
        }

        private Order buildOrder() {
            return Order.builder()
                    .customerId("cust-001")
                    .productId("prod-001")
                    .quantity(2)
                    .totalAmount(new BigDecimal("49.99"))
                    .build();
        }

        private Order buildSavedOrder() {
            return Order.builder()
                    .id(UUID.randomUUID())
                    .customerId("cust-001")
                    .productId("prod-001")
                    .quantity(2)
                    .totalAmount(new BigDecimal("49.99"))
                    .status(OrderStatus.PENDING)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        private OrderResponse buildOrderResponse(Order order) {
            return new OrderResponse(
                    order.getId(),
                    order.getCustomerId(),
                    order.getProductId(),
                    order.getQuantity(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }