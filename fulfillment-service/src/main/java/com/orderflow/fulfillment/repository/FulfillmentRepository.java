package com.orderflow.fulfillment.repository;

import com.orderflow.fulfillment.domain.Fulfillment;
import com.orderflow.fulfillment.domain.FulfillmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, UUID> {
    Optional<Fulfillment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    List<Fulfillment> findByStatus(FulfillmentStatus status);

    List<Fulfillment> findByCustomerId(String customerId);
}
