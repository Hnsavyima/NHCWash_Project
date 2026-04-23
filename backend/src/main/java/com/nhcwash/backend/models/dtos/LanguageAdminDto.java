package com.nhcwash.backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageAdminDto {
    private String code;
    private String displayName;
    private String nativeLabel;
    private String flagEmoji;
    private boolean active;
    private boolean defaultLanguage;
}
