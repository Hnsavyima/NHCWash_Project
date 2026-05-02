package com.nhcwash.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.PreferredLanguageBody;
import com.nhcwash.backend.models.dtos.UserDTO;
import com.nhcwash.backend.models.dtos.UserPatchDTO;
import com.nhcwash.backend.models.dtos.UserUpdateDTO;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.FileStorageService;
import com.nhcwash.backend.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateur", description = "Profil, langue, avatar et suppression de compte")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final DtoConverter dtoConverter;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    @Operation(summary = "Profil du client connecté")
    public ResponseEntity<UserDTO> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(dtoConverter.toUserDto(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/me")
    @Operation(summary = "Mise à jour complète du profil")
    public ResponseEntity<UserDTO> updateMe(@Valid @RequestBody UserUpdateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(
                        dtoConverter.toUserDto(userService.updateProfile(user.getUserId(), dto))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/me")
    @Operation(summary = "Mise à jour partielle du profil")
    public ResponseEntity<UserDTO> patchMe(@Valid @RequestBody UserPatchDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(
                        dtoConverter.toUserDto(userService.patchProfile(user.getUserId(), dto))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /** Updates only {@code preferredLanguage} (FR, EN, NL, DE) — same persistence as PATCH /me with that field set. */
    @PatchMapping("/me/language")
    @Operation(summary = "Langue préférée du compte")
    public ResponseEntity<UserDTO> patchMyLanguage(@Valid @RequestBody PreferredLanguageBody body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserPatchDTO dto = new UserPatchDTO();
        dto.setPreferredLanguage(body.getPreferredLanguage());

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(
                        dtoConverter.toUserDto(userService.patchProfile(user.getUserId(), dto))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Mise à jour de l'avatar")
    public ResponseEntity<UserDTO> patchAvatar(@RequestParam(name = "avatar") MultipartFile avatar) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    String previous = user.getAvatarUrl();
                    String url = fileStorageService.storeAvatar(avatar);
                    fileStorageService.deleteFile(previous);
                    user.setAvatarUrl(url);
                    userRepository.save(user);
                    return ResponseEntity.ok(dtoConverter.toUserDto(user));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /** GDPR soft delete for clients only; sends confirmation email and blocks future logins until admin restore. */
    @DeleteMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT', 'CLIENT')")
    @Operation(summary = "Suppression (soft delete) du compte client")
    public ResponseEntity<Void> deleteMyAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.softDeleteMyAccountByEmail(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/avatar")
    @Operation(summary = "Suppression de l'avatar")
    public ResponseEntity<UserDTO> deleteAvatar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    fileStorageService.deleteFile(user.getAvatarUrl());
                    user.setAvatarUrl(null);
                    userRepository.save(user);
                    return ResponseEntity.ok(dtoConverter.toUserDto(user));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
