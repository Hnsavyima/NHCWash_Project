package com.nhcwash.backend.controllers;

import java.util.List;
import java.util.Optional;

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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/timeslots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<TimeSlotDTO>> listSlots(@RequestParam(required = false) SlotType type) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        if (userRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<TimeSlotDTO> body = timeSlotService.listActiveSlots(Optional.ofNullable(type));
        return ResponseEntity.ok(body);
    }
}
