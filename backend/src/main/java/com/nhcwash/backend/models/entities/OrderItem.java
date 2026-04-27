package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "article_type", length = 80)
    private String articleType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_estimated", precision = 10, scale = 2)
    private BigDecimal unitPriceEstimated;

    @Column(name = "line_total_estimated", precision = 10, scale = 2)
    private BigDecimal lineTotalEstimated;
}
