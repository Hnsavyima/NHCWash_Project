package com.nhcwash.backend.controllers;

import com.nhcwash.backend.configs.JwtUtils;
import com.nhcwash.backend.models.dtos.*;
import com.nhcwash.backend.models.entities.*;
import com.nhcwash.backend.repositories.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // 1. Authentification
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Génération du Token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 3. Récupération des infos utilisateur
        org.springframework.security.core.userdetails.User userDetails = (org.springframework.security.core.userdetails.User) authentication
                .getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Erreur: L'email est déjà utilisé !");
        }

        // Création du nouvel utilisateur
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setPhone(signUpRequest.getPhone());

        // CHIFFREMENT DU MOT DE PASSE (F4)
        user.setPasswordHash(encoder.encode(signUpRequest.getPassword()));

        // Attribution du rôle
        Set<String> strRoles = signUpRequest.getRoles();
        Role role;
        if (strRoles == null || strRoles.isEmpty()) {
            role = roleRepository.findByName("ROLE_CLIENT")
                    .orElseThrow(() -> new RuntimeException("Erreur: Rôle non trouvé."));
        } else {
            String roleName = strRoles.iterator().next();
            role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Erreur: Rôle " + roleName + " non trouvé."));
        }
        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.ok("Utilisateur enregistré avec succès !");
    }
}
