package com.nhcwash.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton row ({@code id = 1}) for site-wide company & contact information.
 */
@Entity
@Table(name = "global_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id = SINGLETON_ID;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 80)
    private String contactPhone;

    @Column(name = "address", nullable = false, length = 1000)
    private String address;

    @Column(name = "vat_number", nullable = false, length = 120)
    private String vatNumber;

    @Column(name = "opening_hours_description", nullable = false, columnDefinition = "TEXT")
    private String openingHoursDescription;

    @Column(name = "support_email", nullable = false, length = 255)
    private String supportEmail;
}
