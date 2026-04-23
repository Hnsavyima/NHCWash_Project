package com.nhcwash.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiteLanguage {

    @Id
    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "native_label", nullable = false, length = 120)
    private String nativeLabel;

    @Column(name = "flag_emoji", nullable = false, length = 8)
    private String flagEmoji;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    private boolean defaultLanguage;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
