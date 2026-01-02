package com.nhcwash.backend.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class OrderRequestDTO {
    private List<ItemRequestDTO> items;
    private String instructions;
}