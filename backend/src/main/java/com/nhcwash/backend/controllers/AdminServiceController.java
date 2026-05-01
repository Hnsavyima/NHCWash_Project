package com.nhcwash.backend.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.ServiceActivePatchDTO;
import com.nhcwash.backend.models.dtos.ServiceCategorySummaryDTO;
import com.nhcwash.backend.models.dtos.ServiceDTO;
import com.nhcwash.backend.models.dtos.ServiceUpsertRequest;
import com.nhcwash.backend.repositories.ServiceCategoryRepository;
import com.nhcwash.backend.repositories.ServiceRepository;
import com.nhcwash.backend.services.AdminCatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/services")
@Tag(name = "Admin — Catalogue services", description = "CRUD et activation des prestations")
@RequiredArgsConstructor
public class AdminServiceController {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final AdminCatalogService adminCatalogService;
    private final DtoConverter dtoConverter;

    @GetMapping("/meta/categories")
    @Operation(summary = "Liste des catégories")
    public ResponseEntity<List<ServiceCategorySummaryDTO>> listCategories() {
        List<ServiceCategorySummaryDTO> body = serviceCategoryRepository.findAll().stream()
                .map(c -> new ServiceCategorySummaryDTO(c.getCategoryId(), c.getName()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping
    @Operation(summary = "Liste de tous les services (admin)")
    public ResponseEntity<List<ServiceDTO>> listAll(@RequestParam(name = "lang", defaultValue = "fr") String lang) {
        List<ServiceDTO> body = serviceRepository.findAll().stream()
                .map(s -> dtoConverter.toServiceDto(s, lang))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping
    @Operation(summary = "Créer un service")
    public ResponseEntity<ServiceDTO> create(@Valid @RequestBody ServiceUpsertRequest dto,
            @RequestParam(name = "lang", defaultValue = "fr") String lang) {
        var created = adminCatalogService.createService(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoConverter.toServiceDto(created, lang));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un service")
    public ResponseEntity<ServiceDTO> update(@PathVariable(name = "id") Long id,
            @Valid @RequestBody ServiceUpsertRequest dto,
            @RequestParam(name = "lang", defaultValue = "fr") String lang) {
        var updated = adminCatalogService.updateService(id, dto);
        return ResponseEntity.ok(dtoConverter.toServiceDto(updated, lang));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activer ou désactiver un service")
    public ResponseEntity<ServiceDTO> patchActive(@PathVariable(name = "id") Long id,
            @Valid @RequestBody ServiceActivePatchDTO dto,
            @RequestParam(name = "lang", defaultValue = "fr") String lang) {
        var updated = adminCatalogService.setServiceActive(id, Boolean.TRUE.equals(dto.getActive()));
        return ResponseEntity.ok(dtoConverter.toServiceDto(updated, lang));
    }
}
