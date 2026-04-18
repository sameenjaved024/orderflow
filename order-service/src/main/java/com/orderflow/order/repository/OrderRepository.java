package com.orderflow.order.repository;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdAndCustomerId(UUID id, String customerId);

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findAllByStatusOrderedByCreatedAtDesc(OrderStatus status);

    boolean existsByIdAndCustomerId(UUID id, String customerId);
}
