package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Status: PENDING, RECEIVED, PROCESSING, READY, DELIVERED, CANCELLED
    private String status;

    @Column(name = "estimated_total", precision = 10, scale = 2)
    private BigDecimal estimatedTotal;

    @Column(name = "final_total", precision = 10, scale = 2)
    private BigDecimal finalTotal;

    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;
}
