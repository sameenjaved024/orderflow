package com.orderflow.order.dto;

import com.orderflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID        id,
        String      customerId,
        String      productId,
        Integer     quantity,
        BigDecimal  totalAmount,
        OrderStatus status,
        Instant     createdAt,
        Instant     updatedAt
) {}
