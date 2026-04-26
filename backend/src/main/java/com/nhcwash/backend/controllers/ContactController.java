package com.nhcwash.backend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.ContactRequest;
import com.nhcwash.backend.services.MailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact", description = "Formulaire de contact public")
@RequiredArgsConstructor
public class ContactController {

    private final MailService mailService;

    @Value("${app.admin.email:}")
    private String adminEmail;

    /** Used when {@code app.admin.email} is blank so dev SMTP can still receive contact mail. */
    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @PostMapping
    @Operation(summary = "Envoyer un message de contact")
    public ResponseEntity<Void> submit(@Valid @RequestBody ContactRequest body) {
        String admin = adminEmail != null ? adminEmail.trim() : "";
        if (admin.isBlank() && smtpUsername != null && !smtpUsername.isBlank()) {
            admin = smtpUsername.trim();
        }
        if (admin.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Formulaire de contact indisponible : adresse administrateur non configurée");
        }
        mailService.sendContactInquiry(admin, body);
        return ResponseEntity.ok().build();
    }
}
