package com.nhcwash.backend.models.dtos;

import java.util.Set;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private Set<String> roles; // Permet de spécifier les rôles à l'inscription
}