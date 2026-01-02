package com.nhcwash.backend.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private String email;
    private List<String> roles;

    public JwtResponse(String token, String email, List<String> roles) {
        this.token = token;
        this.email = email;
        this.roles = roles;
    }
}