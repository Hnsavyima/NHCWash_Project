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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@Tag(
        name = "Admin - i18n",
        description = "Administration des langues et traductions de l'interface."
)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminI18nController {

    private final AdminI18nService adminI18nService;

    @GetMapping("/languages")
    @Operation(
            summary = "Lister les langues (admin)",
            description = "Retourne la liste des langues avec leur état (active/défaut) pour l'administration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié (JWT manquant ou invalide)"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (admin requis)")
    })
    public ResponseEntity<List<LanguageAdminDto>> listLanguages() {
        return ResponseEntity.ok(adminI18nService.listLanguages());
    }

    @PutMapping("/languages/{code}/toggle")
    @Operation(
            summary = "Activer/désactiver une langue",
            description = "Active ou désactive une langue par son code (admin)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Langue mise à jour"),
            @ApiResponse(responseCode = "401", description = "Non authentifié (JWT manquant ou invalide)"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (admin requis)"),
            @ApiResponse(responseCode = "404", description = "Langue introuvable (à confirmer)")
    })
    public ResponseEntity<LanguageAdminDto> toggleLanguage(
            @Parameter(description = "Code langue (ex. fr, en, nl, de).", example = "fr", required = true)
            @PathVariable(name = "code") String code) {
        return ResponseEntity.ok(adminI18nService.toggleLanguage(code));
    }

    @PutMapping("/languages/{code}/default")
    @Operation(
            summary = "Définir la langue par défaut",
            description = "Définit la langue par défaut (admin)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Langue par défaut mise à jour"),
            @ApiResponse(responseCode = "401", description = "Non authentifié (JWT manquant ou invalide)"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (admin requis)"),
            @ApiResponse(responseCode = "404", description = "Langue introuvable (à confirmer)")
    })
    public ResponseEntity<List<LanguageAdminDto>> setDefaultLanguage(
            @Parameter(description = "Code langue (ex. fr, en, nl, de).", example = "fr", required = true)
            @PathVariable(name = "code") String code) {
        return ResponseEntity.ok(adminI18nService.setDefaultLanguage(code));
    }

    @GetMapping("/translations/{langCode}")
    @Operation(
            summary = "Lire les traductions d'une langue",
            description = "Retourne les entrées de traduction pour une langue donnée (admin)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traductions retournées"),
            @ApiResponse(responseCode = "401", description = "Non authentifié (JWT manquant ou invalide)"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (admin requis)"),
            @ApiResponse(responseCode = "404", description = "Langue introuvable (à confirmer)")
    })
    public ResponseEntity<List<TranslationEntryDto>> getTranslations(
            @Parameter(description = "Code langue.", example = "fr", required = true)
            @PathVariable(name = "langCode") String langCode) {
        return ResponseEntity.ok(adminI18nService.getTranslations(langCode));
    }

    @PutMapping("/translations")
    @Operation(
            summary = "Mettre à jour des traductions en masse",
            description = "Met à jour plusieurs entrées de traduction en une seule requête (admin)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Traductions enregistrées"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié (JWT manquant ou invalide)"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (admin requis)")
    })
    public ResponseEntity<Void> putTranslations(@Valid @RequestBody TranslationBulkPutDto body) {
        adminI18nService.putTranslations(body);
        return ResponseEntity.noContent().build();
    }
}
