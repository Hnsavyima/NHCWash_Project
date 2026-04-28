package com.nhcwash.backend.models.dtos;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AdminUserListDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private boolean active;

    @JsonProperty("isDeleted")
    private boolean deleted;

    /** When set, the account is soft-deleted (GDPR archival). */
    private LocalDateTime deletedAt;

    /** Shown on login when the account is suspended. */
    private String suspensionReason;
}
