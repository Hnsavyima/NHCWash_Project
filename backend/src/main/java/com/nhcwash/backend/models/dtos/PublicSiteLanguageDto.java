package com.nhcwash.backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicSiteLanguageDto {
    private String code;
    private String nativeLabel;
}
