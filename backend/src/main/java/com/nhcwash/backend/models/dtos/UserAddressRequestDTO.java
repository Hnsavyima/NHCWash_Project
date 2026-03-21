package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserAddressRequestDTO {

    @Size(max = 50)
    private String label;

    @NotBlank
    @Size(max = 120)
    private String street;

    @Size(max = 20)
    private String number;

    @Size(max = 20)
    private String box;

    @Size(max = 20)
    private String postalCode;

    @NotBlank
    @Size(max = 80)
    private String city;

    @NotBlank
    @Size(max = 80)
    private String country;

    private Boolean defaultAddress;
}
