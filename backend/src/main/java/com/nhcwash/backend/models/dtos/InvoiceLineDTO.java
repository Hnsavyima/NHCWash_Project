package com.nhcwash.backend.models.dtos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class InvoiceLineDTO {
    private Long id;
    private String label;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
