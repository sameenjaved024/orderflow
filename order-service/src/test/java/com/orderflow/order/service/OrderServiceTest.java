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
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(CompletableFuture.completedFuture(null));

            // WHEN
            OrderResponse result = orderService.createOrder(request);

            // THEN
            assertThat(result).isEqualTo(expected);
            then(kafkaTemplate).should()
                    .send(eq("order.created"), anyString(), any(OrderCreatedEvent.class));
        }

        @Test
        @DisplayName("should persist to DB before publishing Kafka event")
        void shouldPersistBeforePublishing() {
            //GIVEN
            given(orderMapper.toEntity(any())).willReturn(buildOrder());
            given(orderRepository.save(any())).willReturn(buildSavedOrder());
            given(orderMapper.toResponse(any())).willReturn(buildOrderResponse(buildSavedOrder()));
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(CompletableFuture.completedFuture(null));

            // WHEN
            orderService.createOrder(buildRequest());

            // THEN — strict ordering matters for data consistency
            InOrder inOrder = inOrder(orderRepository, kafkaTemplate);
            inOrder.verify(orderRepository).save(any());
            inOrder.verify(kafkaTemplate).send(anyString(), anyString(), any());

        }

        @Test
        @DisplayName("should use orderId as Kafka message key")
        void shouldPublishEventWithCorrectKey() {
            // GIVEN
            Order savedOrder = buildSavedOrder();

            given(orderMapper.toEntity(any())).willReturn(buildOrder());
            given(orderRepository.save(any())).willReturn(savedOrder);
            given(orderMapper.toResponse(any()))
                    .willReturn(buildOrderResponse(savedOrder));
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(CompletableFuture.completedFuture(null));

            // WHEN
            orderService.createOrder(buildRequest());

            // THEN — orderId must be the Kafka key for correct partition routing
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(kafkaTemplate).should()
                    .send(anyString(), keyCaptor.capture(), any());
            assertThat(keyCaptor.getValue())
                    .isEqualTo(savedOrder.getId().toString());
        }

        @Test
        @DisplayName("should return order even when Kafka publish fails")
        void shouldReturnResponseEvenIfKafkaFails() {
            // GIVEN
            Order savedOrder = buildSavedOrder();
            OrderResponse expectedResponse = buildOrderResponse(savedOrder);

            given(orderMapper.toEntity(any())).willReturn(buildOrder());
            given(orderRepository.save(any())).willReturn(savedOrder);
            given(orderMapper.toResponse(any())).willReturn(expectedResponse);
            given(kafkaTemplate.send(anyString(), anyString(), any()))
                    .willReturn(CompletableFuture.failedFuture(
                            new RuntimeException("Kafka broker unavailable")));

            // WHEN
            OrderResponse result = orderService.createOrder(buildRequest());

            // THEN — Kafka failure is logged, never thrown to the caller
            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    @Nested
    @DisplayName("findOrderById")
    class FindOrderById {
        @Test

        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() {
            // GIVEN
            Order savedOrder = buildSavedOrder();
            OrderResponse expectedResponse = buildOrderResponse(savedOrder);

            given(orderRepository.findById(savedOrder.getId()))
                    .willReturn(Optional.of(savedOrder));
            given(orderMapper.toResponse(savedOrder))
                    .willReturn(expectedResponse);

            // WHEN
            OrderResponse result = orderService.findOrderById(savedOrder.getId());

            // THEN
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should throw OrderNotFoundException when order does not exist")
        void shouldThrowWhenOrderNotFound() {
            // GIVEN
            UUID unknownId = UUID.randomUUID();
            given(orderRepository.findById(unknownId))
                    .willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> orderService.findOrderById(unknownId))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
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