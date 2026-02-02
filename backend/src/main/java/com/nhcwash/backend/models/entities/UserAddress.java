package com.nhcwash.backend.models.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String label;

    @Column(nullable = false, length = 120)
    private String street;

    @Column(length = 20)
    private String number;

    @Column(length = 20)
    private String box;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
