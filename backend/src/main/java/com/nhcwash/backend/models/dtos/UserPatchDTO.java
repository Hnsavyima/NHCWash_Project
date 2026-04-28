package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Partial update for PATCH /api/users/me — only non-null fields are applied. */
@Data
public class UserPatchDTO {

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Size(max = 30)
    private String phone;

    @Size(max = 8)
    private String preferredLanguage;
}
