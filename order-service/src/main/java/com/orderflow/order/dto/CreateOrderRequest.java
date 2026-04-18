package com.orderflow.order.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Validated request DTO for creating a new order.
 * Uses Java record — immutable by design.
 */
public record CreateOrderRequest(

        @NotBlank(message = "Customer ID must not be blank")
        String customerId,

        @NotBlank(message = "Product ID must not be blank")
        String productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 1000, message = "Quantity must not exceed 1000")
        Integer quantity,

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.01", message = "Total amount must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Total amount format is invalid")
        BigDecimal totalAmount
) {}
