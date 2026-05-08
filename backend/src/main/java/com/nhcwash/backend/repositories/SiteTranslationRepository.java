package com.nhcwash.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.SiteTranslation;

public interface SiteTranslationRepository extends JpaRepository<SiteTranslation, Long> {

    List<SiteTranslation> findByLangCodeOrderByMsgKeyAsc(String langCode);

    Optional<SiteTranslation> findByLangCodeAndMsgKey(String langCode, String msgKey);

    long countByLangCode(String langCode);
}
