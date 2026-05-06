package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GlobalSettingsDto {

    @NotBlank
    @Size(max = 200)
    private String companyName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String contactEmail;

    @NotBlank
    @Size(max = 80)
    private String contactPhone;

    @NotBlank
    @Size(max = 1000)
    private String address;

    @NotBlank
    @Size(max = 120)
    private String vatNumber;

    @Size(max = 8000)
    private String openingHoursDescription;

    @NotBlank
    @Email
    @Size(max = 255)
    private String supportEmail;
}
