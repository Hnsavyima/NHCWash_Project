package com.nhcwash.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.ApiKeyListDto;
import com.nhcwash.backend.models.dtos.CreateApiKeyRequest;
import com.nhcwash.backend.models.dtos.CreateApiKeyResponseDto;
import com.nhcwash.backend.services.ApiKeyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/keys")
@Tag(name = "Admin — Clés API", description = "Gestion des clés d'intégration")
@RequiredArgsConstructor
public class AdminApiController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @Operation(summary = "Liste des clés API")
    public ResponseEntity<List<ApiKeyListDto>> listKeys() {
        return ResponseEntity.ok(apiKeyService.listKeys());
    }

    @PostMapping
    @Operation(summary = "Créer une clé API")
    public ResponseEntity<CreateApiKeyResponseDto> createKey(@Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.createKey(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Révoquer une clé API")
    public ResponseEntity<Void> deleteKey(@PathVariable(name = "id") Long id) {
        apiKeyService.deleteKey(id);
        return ResponseEntity.noContent().build();
    }
}
