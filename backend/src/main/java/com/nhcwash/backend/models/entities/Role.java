package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Column(nullable = false, length = 30)
    private String name; // ROLE_CLIENT | ROLE_EMPLOYEE | ROLE_ADMIN (Spring Security convention)
}
