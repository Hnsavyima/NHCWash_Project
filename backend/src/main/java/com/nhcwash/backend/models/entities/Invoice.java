package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "pdf_path", length = 255)
    private String pdfPath;
}
