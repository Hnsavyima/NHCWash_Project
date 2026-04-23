package com.nhcwash.backend.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.nhcwash.backend.services.AdminI18nService;

import lombok.RequiredArgsConstructor;

/**
 * Seeds {@code site_languages} and {@code site_translations} from {@code classpath:i18n-seed/*.json} when tables are empty.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class SiteI18nDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteI18nDataInitializer.class);

    private final AdminI18nService adminI18nService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            adminI18nService.seedFromClasspathIfEmpty();
            adminI18nService.ensureSupportedLanguagesAndMissingTranslations();
        } catch (Exception e) {
            log.warn("Site i18n seed skipped or failed: {}", e.getMessage());
        }
    }
}
