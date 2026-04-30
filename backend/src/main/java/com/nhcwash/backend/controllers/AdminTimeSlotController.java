package com.nhcwash.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.TimeSlotBatchRequest;
import com.nhcwash.backend.models.dtos.TimeSlotDTO;
import com.nhcwash.backend.models.dtos.TimeSlotDeleteResultDTO;
import com.nhcwash.backend.models.dtos.TimeSlotUpdateRequest;
import com.nhcwash.backend.services.AdminTimeSlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/timeslots")
@Tag(name = "Admin — Créneaux", description = "Gestion des plages horaires")
@RequiredArgsConstructor
public class AdminTimeSlotController {

    private final AdminTimeSlotService adminTimeSlotService;

    @GetMapping
    @Operation(summary = "Liste de tous les créneaux")
    public ResponseEntity<List<TimeSlotDTO>> listAll() {
        return ResponseEntity.ok(adminTimeSlotService.listAllOrderedByStart());
    }

    @PostMapping("/batch")
    @Operation(summary = "Générer des créneaux par lot")
    public ResponseEntity<List<TimeSlotDTO>> generateBatch(@Valid @RequestBody TimeSlotBatchRequest body) {
        List<TimeSlotDTO> created = adminTimeSlotService.generateBatch(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un créneau")
    public ResponseEntity<TimeSlotDTO> update(@PathVariable(name = "id") Long id, @Valid @RequestBody TimeSlotUpdateRequest body) {
        return ResponseEntity.ok(adminTimeSlotService.update(id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer ou désactiver un créneau")
    public ResponseEntity<TimeSlotDeleteResultDTO> delete(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(adminTimeSlotService.deleteOrDeactivate(id));
    }
}
