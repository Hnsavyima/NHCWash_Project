package com.nhcwash.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.LanguageAdminDto;
import com.nhcwash.backend.models.dtos.TranslationBulkPutDto;
import com.nhcwash.backend.models.dtos.TranslationEntryDto;
import com.nhcwash.backend.services.AdminI18nService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminI18nController {

    private final AdminI18nService adminI18nService;

    @GetMapping("/languages")
    public ResponseEntity<List<LanguageAdminDto>> listLanguages() {
        return ResponseEntity.ok(adminI18nService.listLanguages());
    }

    @PutMapping("/languages/{code}/toggle")
    public ResponseEntity<LanguageAdminDto> toggleLanguage(@PathVariable(name = "code") String code) {
        return ResponseEntity.ok(adminI18nService.toggleLanguage(code));
    }

    @PutMapping("/languages/{code}/default")
    public ResponseEntity<List<LanguageAdminDto>> setDefaultLanguage(@PathVariable(name = "code") String code) {
        return ResponseEntity.ok(adminI18nService.setDefaultLanguage(code));
    }

    @GetMapping("/translations/{langCode}")
    public ResponseEntity<List<TranslationEntryDto>> getTranslations(@PathVariable(name = "langCode") String langCode) {
        return ResponseEntity.ok(adminI18nService.getTranslations(langCode));
    }

    @PutMapping("/translations")
    public ResponseEntity<Void> putTranslations(@Valid @RequestBody TranslationBulkPutDto body) {
        adminI18nService.putTranslations(body);
        return ResponseEntity.noContent().build();
    }
}
