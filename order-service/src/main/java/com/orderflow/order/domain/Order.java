package com.orderflow.order.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
                @Index(name = "idx_orders_status",      columnList = "status"),
                @Index(name = "idx_orders_created_at",  columnList = "created_at")
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "totalAmount")
public class Order {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(name = "id", updatable = false, nullable = false)
        private UUID id;

        @Column(name = "customer_id", nullable = false)
        private String customerId;

        @Column(name = "product_id", nullable = false)
        private String productId;

        @Column(name = "quantity", nullable = false)
        private Integer quantity;

        @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
        private BigDecimal totalAmount;

        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false, length = 20)
        private OrderStatus status;

        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

        @Version
        @Column(name = "version")
        private Long version;

        @PrePersist
        protected void onPersist() {
                Instant now = Instant.now();
                this.createdAt = now;
                this.updatedAt = now;
                if (this.status == null) {
                        this.status = OrderStatus.PENDING;
                }
        }

        @PreUpdate
        protected void onUpdate() {
                this.updatedAt = Instant.now();
        }
}
