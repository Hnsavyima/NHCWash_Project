package com.nhcwash.backend.models.dtos;

import lombok.Data;

@Data
public class OrderItemDTO {
    private String serviceName; // Selon la langue
    private Integer quantity;
    private Double unitPrice;
}