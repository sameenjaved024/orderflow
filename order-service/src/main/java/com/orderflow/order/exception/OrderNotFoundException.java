package com.orderflow.order.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException{
    public OrderNotFoundException(UUID orderId) {
        super("Order not found with id: " + orderId);
    }

    public OrderNotFoundException(UUID orderId, String customerId) {
        super("Order not found with id: " + orderId + " for customer: " + customerId);
    }
}
