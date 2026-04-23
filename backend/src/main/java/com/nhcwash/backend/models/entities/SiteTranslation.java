package com.nhcwash.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site_translations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_site_translation_lang_key", columnNames = { "lang_code", "msg_key" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiteTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lang_code", nullable = false, length = 8)
    private String langCode;

    @Column(name = "msg_key", nullable = false, length = 512)
    private String msgKey;

    @Column(name = "msg_value", nullable = false, columnDefinition = "TEXT")
    private String msgValue;
}
