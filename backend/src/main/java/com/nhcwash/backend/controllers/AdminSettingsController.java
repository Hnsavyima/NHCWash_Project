package com.nhcwash.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.GlobalSettingsDto;
import com.nhcwash.backend.services.GlobalSettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/settings")
@Tag(name = "Admin — Paramètres", description = "Configuration globale de l'application")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final GlobalSettingsService globalSettingsService;

    @GetMapping
    @Operation(summary = "Lire les paramètres globaux")
    public ResponseEntity<GlobalSettingsDto> getSettings() {
        return ResponseEntity.ok(globalSettingsService.getSettingsDto());
    }

    @PutMapping
    @Operation(summary = "Mettre à jour les paramètres globaux")
    public ResponseEntity<GlobalSettingsDto> updateSettings(@Valid @RequestBody GlobalSettingsDto dto) {
        return ResponseEntity.ok(globalSettingsService.updateSettings(dto));
    }
}
