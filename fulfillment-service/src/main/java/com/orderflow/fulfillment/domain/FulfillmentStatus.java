package com.orderflow.fulfillment.domain;

public enum FulfillmentStatus {
    RECEIVED,
    PROCESSING,
    PACKED,
    DISPATCHED,
    DELIVERED,
    FAILED
}
