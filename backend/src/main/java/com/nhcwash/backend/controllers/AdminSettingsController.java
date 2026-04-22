package com.nhcwash.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.GlobalSettingsDto;
import com.nhcwash.backend.services.GlobalSettingsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final GlobalSettingsService globalSettingsService;

    @GetMapping
    public ResponseEntity<GlobalSettingsDto> getSettings() {
        return ResponseEntity.ok(globalSettingsService.getSettingsDto());
    }

    @PutMapping
    public ResponseEntity<GlobalSettingsDto> updateSettings(@Valid @RequestBody GlobalSettingsDto dto) {
        return ResponseEntity.ok(globalSettingsService.updateSettings(dto));
    }
}
