package com.nhcwash.backend.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class InvoiceDTO {
    private Long id;
    private Long orderId;
    private String invoiceNumber;
    private LocalDateTime issuedAt;
    private BigDecimal vatRate;
    private BigDecimal subtotal;
    private BigDecimal vatAmount;
    private BigDecimal total;
    private String pdfPath;
    private List<InvoiceLineDTO> lines;
}
