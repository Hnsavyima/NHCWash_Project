package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.constants.PasswordRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmDto {

    @NotBlank
    private String token;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
    private String newPassword;
}
