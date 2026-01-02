package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Double amount;
    private LocalDateTime paymentDate;
    private String method; // ONLINE_CARD, CASH, POS_TERMINAL
    private String status; // PAID, UNPAID, REFUNDED
}
