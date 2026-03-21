package com.nhcwash.backend.models.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class OrderRequestDTO {
    @NotEmpty
    @Valid
    private List<ItemRequestDTO> items;

    private String instructions;
}
