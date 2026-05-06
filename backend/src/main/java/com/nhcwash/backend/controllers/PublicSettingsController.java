package com.nhcwash.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.GlobalSettingsDto;
import com.nhcwash.backend.services.GlobalSettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/settings")
@Tag(name = "Réglages publics", description = "Paramètres globaux visibles sur le site")
@RequiredArgsConstructor
public class PublicSettingsController {

    private final GlobalSettingsService globalSettingsService;

    @GetMapping
    @Operation(summary = "Lire les réglages publics")
    public ResponseEntity<GlobalSettingsDto> getPublicSettings() {
        return ResponseEntity.ok(globalSettingsService.getSettingsDto());
    }
}
