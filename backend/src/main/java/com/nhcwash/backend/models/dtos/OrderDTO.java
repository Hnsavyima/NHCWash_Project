package com.nhcwash.backend.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private String status;
    private Double totalPrice;
    private String createdAt;
    private String instructions;
    private List<OrderItemDTO> items;
    private String paymentStatus;
}
