package com.nhcwash.backend.models.dtos;

import lombok.Data;

@Data
public class UserAddressDTO {
    private Long id;
    private String label;
    private String street;
    private String number;
    private String box;
    private String postalCode;
    private String city;
    private String country;
    private Boolean defaultAddress;
}
