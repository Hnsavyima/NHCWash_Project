package com.nhcwash.backend.models.dtos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CreateApiKeyResponseDto {
    private Long id;
    private String name;
    /** Plain secret — returned only on creation. */
    private String key;
    private LocalDateTime createdAt;
}
