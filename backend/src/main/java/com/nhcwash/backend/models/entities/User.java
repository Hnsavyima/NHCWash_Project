package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(unique = true, length = 30)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Soft-delete: archived user, hidden from normal flows; admin may restore. */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /** GDPR soft-delete timestamp; when set, login is blocked until admin restore. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Optional message shown to the user when the account is suspended (inactive but not archived). */
    @Column(name = "suspension_reason", length = 2000)
    private String suspensionReason;

    /** Public URL path for static serving, e.g. /uploads/avatars/{uuid}.jpg */
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /** UI / email locale: FR, EN, NL, or DE */
    @Column(name = "preferred_language", length = 8)
    private String preferredLanguage = "FR";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "client")
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (preferredLanguage == null || preferredLanguage.isBlank()) {
            preferredLanguage = "FR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
