package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for PATCH /api/users/me/language */
@Data
public class PreferredLanguageBody {

    @NotBlank
    @Size(max = 8)
    private String preferredLanguage;
}
