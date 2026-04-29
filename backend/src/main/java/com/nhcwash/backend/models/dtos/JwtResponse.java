package com.nhcwash.backend.models.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String email;
    private List<String> roles;
    /**
     * User's persisted UI locale from the database (normalized to FR, EN, or NL).
     * Frontend maps this to i18n (fr / en / nl) immediately after login.
     */
    private String preferredLanguage;
}
