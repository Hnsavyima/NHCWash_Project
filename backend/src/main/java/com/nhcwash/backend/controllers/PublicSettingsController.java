package com.nhcwash.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.GlobalSettingsDto;
import com.nhcwash.backend.services.GlobalSettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/settings")
@RequiredArgsConstructor
public class PublicSettingsController {

    private final GlobalSettingsService globalSettingsService;

    @GetMapping
    public ResponseEntity<GlobalSettingsDto> getPublicSettings() {
        return ResponseEntity.ok(globalSettingsService.getSettingsDto());
    }
}
