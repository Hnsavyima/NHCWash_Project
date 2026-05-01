package com.nhcwash.backend.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.TimeSlotDTO;
import com.nhcwash.backend.models.enumerations.SlotType;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.TimeSlotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timeslots")
@Tag(name = "Créneaux", description = "Créneaux disponibles pour réservation")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Liste des créneaux actifs")
    public ResponseEntity<List<TimeSlotDTO>> listSlots(
            @RequestParam(name = "type", required = false) SlotType type,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        if (userRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<TimeSlotDTO> body = date != null
                ? timeSlotService.listActiveSlotsForDate(date, Optional.ofNullable(type))
                : timeSlotService.listActiveSlots(Optional.ofNullable(type));
        return ResponseEntity.ok(body);
    }
}
