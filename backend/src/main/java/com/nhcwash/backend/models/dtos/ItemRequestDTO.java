package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemRequestDTO {
    @NotNull
    private Long serviceId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
