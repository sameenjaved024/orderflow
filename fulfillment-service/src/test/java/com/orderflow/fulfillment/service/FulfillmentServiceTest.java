package com.orderflow.fulfillment.service;

import com.orderflow.fulfillment.domain.Fulfillment;
import com.orderflow.fulfillment.domain.FulfillmentStatus;
import com.orderflow.fulfillment.event.OrderCreatedEvent;
import com.orderflow.fulfillment.repository.FulfillmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FulfillmentService")
class FulfillmentServiceTest {

    @Mock
    private FulfillmentRepository fulfillmentRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private FulfillmentService fulfillmentService;

    @BeforeEach
    void setUp() {
        fulfillmentService = new FulfillmentService(
                fulfillmentRepository, rabbitTemplate);
    }

    @Nested
    @DisplayName("processOrder")
    class ProcessOrder {

        @Test
        @DisplayName("should create fulfillment record and publish RabbitMQ event")
        void shouldCreateFulfillmentAndPublishEvent() {
            // GIVEN
            OrderCreatedEvent event = buildOrderCreatedEvent();
            given(fulfillmentRepository.existsByOrderId(event.orderId()))
                    .willReturn(false);
            given(fulfillmentRepository.save(any(Fulfillment.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // WHEN
            fulfillmentService.processOrder(event);

            // THEN
            then(fulfillmentRepository).should(atLeastOnce())
                    .save(any(Fulfillment.class));
            then(rabbitTemplate).should()
                    .convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("should save fulfillment with DISPATCHED status")
        void shouldSaveFulfillmentWithDispatchedStatus() {
            // GIVEN
            OrderCreatedEvent event = buildOrderCreatedEvent();
            given(fulfillmentRepository.existsByOrderId(event.orderId()))
                    .willReturn(false);

            ArgumentCaptor<Fulfillment> captor =
                    ArgumentCaptor.forClass(Fulfillment.class);
            given(fulfillmentRepository.save(captor.capture()))
                    .willAnswer(inv -> inv.getArgument(0));

            // WHEN
            fulfillmentService.processOrder(event);

            // THEN — last saved fulfillment must be DISPATCHED
            Fulfillment lastSaved = captor.getValue();
            assertThat(lastSaved.getStatus())
                    .isEqualTo(FulfillmentStatus.DISPATCHED);
        }

        @Test
        @DisplayName("should skip duplicate event — idempotency check")
        void shouldSkipDuplicateEvent() {
            // GIVEN — fulfillment already exists for this orderId
            OrderCreatedEvent event = buildOrderCreatedEvent();
            given(fulfillmentRepository.existsByOrderId(event.orderId()))
                    .willReturn(true);

            // WHEN
            fulfillmentService.processOrder(event);

            // THEN — nothing saved, nothing published
            then(fulfillmentRepository).should(never())
                    .save(any(Fulfillment.class));
            then(rabbitTemplate).should(never())
                    .convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    // ── Builder ───────────────────────────────────────────────────

    private OrderCreatedEvent buildOrderCreatedEvent() {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                "cust-001",
                "prod-001",
                2,
                new BigDecimal("49.99"),
                Instant.now()
        );
    }
}