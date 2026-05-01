package com.nhcwash.backend.controllers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.AdminUserListDTO;
import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.UserActivePatchDTO;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin — Utilisateurs", description = "Liste et modération des comptes")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final DtoConverter dtoConverter;

    @GetMapping
    @Operation(summary = "Liste des utilisateurs")
    public ResponseEntity<List<AdminUserListDTO>> listUsers() {
        Long actorId = currentUserId();
        List<AdminUserListDTO> body = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getUserId))
                .filter(u -> actorId == null || !u.getUserId().equals(actorId))
                .map(dtoConverter::toAdminUserListDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activer ou désactiver un utilisateur")
    public ResponseEntity<AdminUserListDTO> patchActive(@PathVariable(name = "id") Long id,
            @Valid @RequestBody UserActivePatchDTO dto) {
        User updated = userService.setUserActive(id, Boolean.TRUE.equals(dto.getActive()), currentUserId(),
                dto.getReason());
        return ResponseEntity.ok(dtoConverter.toAdminUserListDto(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archiver un utilisateur (soft delete)")
    public ResponseEntity<AdminUserListDTO> softDelete(@PathVariable(name = "id") Long id) {
        Long actorId = currentUserId();
        User updated = userService.softDeleteUser(id, actorId);
        return ResponseEntity.ok(dtoConverter.toAdminUserListDto(updated));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restaurer un compte archivé")
    public ResponseEntity<AdminUserListDTO> restore(@PathVariable(name = "id") Long id) {
        User updated = userService.restoreUser(id);
        return ResponseEntity.ok(dtoConverter.toAdminUserListDto(updated));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getUserId).orElse(null);
    }
}
