package com.nhcwash.backend.configs;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.nhcwash.backend.models.constants.RoleNames;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
public class JwtUtils {

    @Value("${nhcwash.app.jwt-secret}")
    private String jwtSecret;

    @Value("${nhcwash.app.jwt-expiration-ms}")
    private int jwtExpirationMs;

    // Crée une clé sécurisée à partir de la String jwtSecret
    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256) // Utilisation de la nouvelle méthode signWith
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder() // parser() -> parserBuilder()
                .setSigningKey(key()) // setSigningKey(String) -> setSigningKey(Key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public List<GrantedAuthority> getAuthoritiesFromJwtToken(String token) {
        Object raw = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof List<?> list)) {
            return null;
        }
        List<GrantedAuthority> out = new ArrayList<>();
        for (Object el : list) {
            if (el != null) {
                String norm = normalizeAuthority(String.valueOf(el));
                if (norm != null) {
                    out.add(new SimpleGrantedAuthority(norm));
                }
            }
        }
        return out.isEmpty() ? null : out;
    }

    /**
     * Maps JWT role strings to Spring {@code GrantedAuthority} names ({@code ROLE_*}).
     */
    private static String normalizeAuthority(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("ROLE_")) {
            return s;
        }
        String upper = s.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ADMIN" -> RoleNames.ADMIN;
            case "EMPLOYEE" -> RoleNames.EMPLOYEE;
            case "CLIENT" -> RoleNames.CLIENT;
            default -> "ROLE_" + upper;
        };
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }
}