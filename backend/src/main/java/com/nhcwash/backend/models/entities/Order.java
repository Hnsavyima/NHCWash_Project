package com.nhcwash.backend.models.entities;

import com.nhcwash.backend.models.enumerations.CheckoutPaymentMode;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id", nullable = false)
    private User client;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status; // PENDING, RECEIVED, PROCESSING, READY, DELIVERED, CANCELLED

    /** ONLINE = Stripe checkout; CASH_ON_SITE = pay at counter (no Stripe session). */
    @Enumerated(EnumType.STRING)
    @Column(name = "checkout_payment_mode", length = 20)
    private CheckoutPaymentMode checkoutPaymentMode;

    @Column(name = "estimated_total", precision = 10, scale = 2)
    private BigDecimal estimatedTotal;

    @Column(name = "final_total", precision = 10, scale = 2)
    private BigDecimal finalTotal;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private Set<Payment> payments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<Appointment> appointments = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Invoice invoice;

    /** When the order was refunded (Stripe or manual). */
    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    /** e.g. STRIPE_API, MANUAL_CASH, MANUAL_POS */
    @Column(name = "refund_method", length = 40)
    private String refundMethod;

    /** Stripe refund id (re_…) or free-text note for manual refunds. */
    @Column(name = "refund_reference", length = 500)
    private String refundReference;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Chargeable / display total: finalized amount when set, otherwise estimate, otherwise sum of line totals.
     */
    public BigDecimal getTotalAmount() {
        if (finalTotal != null) {
            return finalTotal;
        }
        if (estimatedTotal != null) {
            return estimatedTotal;
        }
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(OrderItem::getLineTotalEstimated)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
