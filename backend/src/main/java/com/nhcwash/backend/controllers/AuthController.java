package com.nhcwash.backend.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nhcwash.backend.configs.JwtUtils;
import com.nhcwash.backend.models.constants.RoleNames;
import com.nhcwash.backend.models.dtos.JwtResponse;
import com.nhcwash.backend.models.dtos.LoginRequest;
import com.nhcwash.backend.models.dtos.MultipartSignupRequest;
import com.nhcwash.backend.models.dtos.PasswordResetConfirmDto;
import com.nhcwash.backend.models.dtos.PasswordResetRequestDto;
import com.nhcwash.backend.models.entities.Role;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.RoleRepository;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.FileStorageService;
import com.nhcwash.backend.services.MailService;
import com.nhcwash.backend.services.PasswordResetService;
import com.nhcwash.backend.util.LanguagePreference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Connexion, inscription et réinitialisation du mot de passe")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final FileStorageService fileStorageService;
    private final MailService mailService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Connexion (obtention d'un JWT)")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);

            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            String preferred = userRepository.findByEmail(loginRequest.getEmail())
                    .map(u -> LanguagePreference.normalize(u.getPreferredLanguage()))
                    .orElse("FR");

            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), roles, preferred));
        } catch (LockedException e) {
            // Spring checks accountNonLocked before enabled: soft-deleted users hit LockedException, not Disabled.
            String emailKey = loginRequest.getEmail() == null ? "" : loginRequest.getEmail().trim();
            if (!emailKey.isBlank()) {
                var opt = userRepository.findByEmail(emailKey);
                if (opt.isPresent()) {
                    User u = opt.get();
                    boolean softDeleted = u.getDeletedAt() != null || Boolean.TRUE.equals(u.getIsDeleted());
                    if (softDeleted) {
                        String reason = "Votre compte a été archivé. Vos données sont programmées pour suppression (RGPD). Contactez le support en cas d'erreur.";
                        System.out.println("Sending suspension reason (locked/archived): " + u.getSuspensionReason());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                                "error", "ACCOUNT_SUSPENDED",
                                "message", "Ce compte n'est plus accessible.",
                                "reason", reason));
                    }
                }
            }
            String msg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Ce compte a été désactivé. Veuillez contacter le support.";
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(singleMessage(msg));
        } catch (DisabledException e) {
            String emailKey = loginRequest.getEmail() == null ? "" : loginRequest.getEmail().trim();
            User user = null;
            if (!emailKey.isBlank()) {
                user = userRepository.findByEmail(emailKey).orElse(null);
            }
            String reason = user != null && user.getSuspensionReason() != null ? user.getSuspensionReason() : "";
            System.out.println("Sending suspension reason: " + (user != null ? user.getSuspensionReason() : null));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "ACCOUNT_SUSPENDED",
                    "message", "Ce compte est désactivé.",
                    "reason", reason));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(singleMessage("Email ou mot de passe incorrect"));
        }
    }

    private static Map<String, String> singleMessage(String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("message", message);
        return m;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Inscription d'un nouveau client")
    public ResponseEntity<String> registerUser(
            @Valid @ModelAttribute MultipartSignupRequest signUpRequest,
            @RequestParam(name = "avatar", required = false) MultipartFile avatar) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Erreur: L'email est déjà utilisé !");
        }

        String phoneTrimmed = signUpRequest.getPhone().trim();
        if (userRepository.existsByPhone(phoneTrimmed)) {
            return ResponseEntity.badRequest().body("Erreur: Ce numéro de téléphone est déjà utilisé !");
        }

        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setPhone(phoneTrimmed);

        user.setPasswordHash(encoder.encode(signUpRequest.getPassword()));
        user.setPreferredLanguage(LanguagePreference.normalize(signUpRequest.getPreferredLanguage()));

        Role role = roleRepository.findByName(RoleNames.CLIENT).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleNames.CLIENT);
            return roleRepository.save(r);
        });
        user.setRole(role);
        userRepository.save(user);

        if (avatar != null && !avatar.isEmpty()) {
            String url = fileStorageService.storeAvatar(avatar);
            user.setAvatarUrl(url);
            userRepository.save(user);
        }

        mailService.sendWelcomeEmail(user);

        return ResponseEntity.ok("Utilisateur enregistré avec succès !");
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Demande de réinitialisation du mot de passe")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto dto) {
        passwordResetService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirmation du nouveau mot de passe (token)")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDto dto) {
        passwordResetService.confirmPasswordReset(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
