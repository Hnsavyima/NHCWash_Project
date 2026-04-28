package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApiKeyRequest {

    @NotBlank
    @Size(max = 200)
    private String name;
}
