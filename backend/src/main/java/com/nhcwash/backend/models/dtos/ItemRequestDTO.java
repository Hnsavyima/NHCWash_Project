package com.nhcwash.backend.models.dtos;

import lombok.Data;

@Data
public class ItemRequestDTO {
    private Long serviceId;
    private Integer quantity;
}