package com.nhcwash.backend.models.dtos;

import lombok.Data;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    /** Persisted locale for UI and emails: FR, EN, NL, DE */
    private String preferredLanguage;
    private String avatarUrl;
    private Set<String> roles; // On ne renvoie que les noms des rôles
}
