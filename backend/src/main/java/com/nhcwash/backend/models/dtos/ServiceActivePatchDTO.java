package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceActivePatchDTO {
    @NotNull
    private Boolean active;
}
