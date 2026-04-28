package com.nhcwash.backend.models.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceUpsertRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal basePrice;

    @NotNull
    private Long categoryId;

    @Min(0)
    private Integer estimatedDelayHours;

    private Boolean active;
}
