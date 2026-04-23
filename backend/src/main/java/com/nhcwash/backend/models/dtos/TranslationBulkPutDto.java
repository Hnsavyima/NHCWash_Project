package com.nhcwash.backend.models.dtos;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TranslationBulkPutDto {

    @NotBlank
    private String langCode;

    @NotNull
    private Map<String, String> values = new LinkedHashMap<>();
}
