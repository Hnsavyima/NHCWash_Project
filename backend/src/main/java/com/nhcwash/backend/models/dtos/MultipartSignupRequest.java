package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.constants.PasswordRules;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MultipartSignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank(message = "error.phone.required")
    @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]{8,22}$", message = "error.phone.invalid")
    private String phone;

    /** Optional: FR, EN, NL (case-insensitive). Defaults to FR when absent. */
    @Size(max = 8)
    private String preferredLanguage;
}
