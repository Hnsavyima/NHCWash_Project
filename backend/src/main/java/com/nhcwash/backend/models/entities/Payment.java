package com.nhcwash.backend.models.entities;

import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.enumerations.PaymentProvider;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "provider_tx_id", length = 120)
    private String providerTxId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider; // STRIPE | PAYPAL | ONSITE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method; // CARD | CASH | POS_TERMINAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status; // PENDING | SUCCEEDED | FAILED | REFUNDED

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
