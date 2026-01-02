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
    private String languagePreference;
    private Set<String> roles; // On ne renvoie que les noms des rôles
}