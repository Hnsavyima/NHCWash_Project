package com.nhcwash.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.PublicSiteLanguageDto;
import com.nhcwash.backend.services.AdminI18nService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@Tag(
        name = "Langues publiques",
        description = "Langues actives disponibles pour l'interface publique (Open Data / données publiques)."
)
@RequiredArgsConstructor
public class PublicSiteLanguagesController {

    private final AdminI18nService adminI18nService;

    @GetMapping("/site-languages")
    @Operation(
            summary = "Lister les langues actives du site (public)",
            description = """
                    Retourne la liste des langues actives disponibles pour l'interface publique.

                    Cet endpoint est public (sans JWT) et peut être utilisé pour alimenter un sélecteur de langue.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des langues retournée avec succès")
    })
    public ResponseEntity<List<PublicSiteLanguageDto>> activeLanguages() {
        return ResponseEntity.ok(adminI18nService.listPublicActiveLanguages());
    }
}
