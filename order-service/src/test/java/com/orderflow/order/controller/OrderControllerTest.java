package com.orderflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private OrderService orderService;

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrder {

        @Test
        @DisplayName("should return 201 with created order")
        void shouldReturn201WhenOrderCreated() throws Exception {
            // GIVEN
            CreateOrderRequest request = buildRequest();
            OrderResponse response = buildResponse();

            given(orderService.createOrder(any())).willReturn(response);

            // WHEN / THEN
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value("cust-001"))
                    .andExpect(jsonPath("$.productId").value("prod-001"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    // ── Builders ─────────────────────────────────────────────────

    private CreateOrderRequest buildRequest() {
        return new CreateOrderRequest(
                "cust-001", "prod-001", 2, new BigDecimal("49.99"));
    }

    private OrderResponse buildResponse() {
        return new OrderResponse(
                UUID.randomUUID(),
                "cust-001",
                "prod-001",
                2,
                new BigDecimal("49.99"),
                OrderStatus.PENDING,
                Instant.now(),
                Instant.now()
        );
    }
}