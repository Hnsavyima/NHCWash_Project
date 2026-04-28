package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserActivePatchDTO {
    @NotNull
    private Boolean active;

    /** Optional explanation stored when {@code active} is set to {@code false}. */
    private String reason;
}
