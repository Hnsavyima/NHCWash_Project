package com.nhcwash.backend.models.dtos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ApiKeyListDto {
    private Long id;
    private String name;
    private String maskedKey;
    private LocalDateTime createdAt;
    private boolean active;
}
