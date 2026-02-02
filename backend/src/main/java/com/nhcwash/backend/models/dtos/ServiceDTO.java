package com.nhcwash.backend.models.dtos;

import lombok.Data;

@Data
public class ServiceDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
}
