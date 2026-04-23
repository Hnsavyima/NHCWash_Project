package com.nhcwash.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.PublicSiteLanguageDto;
import com.nhcwash.backend.services.AdminI18nService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicSiteLanguagesController {

    private final AdminI18nService adminI18nService;

    @GetMapping("/site-languages")
    public ResponseEntity<List<PublicSiteLanguageDto>> activeLanguages() {
        return ResponseEntity.ok(adminI18nService.listPublicActiveLanguages());
    }
}
