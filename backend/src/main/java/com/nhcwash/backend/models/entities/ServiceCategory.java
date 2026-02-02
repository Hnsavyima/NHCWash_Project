package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
