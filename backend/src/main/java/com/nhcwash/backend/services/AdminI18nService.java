package com.nhcwash.backend.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhcwash.backend.models.dtos.LanguageAdminDto;
import com.nhcwash.backend.models.dtos.PublicSiteLanguageDto;
import com.nhcwash.backend.models.dtos.TranslationBulkPutDto;
import com.nhcwash.backend.models.dtos.TranslationEntryDto;
import com.nhcwash.backend.models.entities.SiteLanguage;
import com.nhcwash.backend.models.entities.SiteTranslation;
import com.nhcwash.backend.repositories.SiteLanguageRepository;
import com.nhcwash.backend.repositories.SiteTranslationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminI18nService {

    private static final Set<String> ALLOWED_LANG = Set.of("fr", "en", "nl", "de");

    private final SiteLanguageRepository siteLanguageRepository;
    private final SiteTranslationRepository siteTranslationRepository;
    private final ObjectMapper objectMapper;

    public List<LanguageAdminDto> listLanguages() {
        return siteLanguageRepository.findAllByOrderBySortOrderAsc().stream().map(this::toLangDto).toList();
    }

    public List<PublicSiteLanguageDto> listPublicActiveLanguages() {
        return siteLanguageRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(l -> new PublicSiteLanguageDto(l.getCode(), l.getNativeLabel()))
                .toList();
    }

    @Transactional
    public LanguageAdminDto toggleLanguage(String rawCode) {
        String code = normalizeCode(rawCode);
        SiteLanguage lang = siteLanguageRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown language: " + code));
        if (lang.isActive()) {
            if (siteLanguageRepository.countByActiveTrue() <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one language must remain active");
            }
            if (lang.isDefaultLanguage()) {
                lang.setDefaultLanguage(false);
                SiteLanguage nextDefault = siteLanguageRepository.findAllByOrderBySortOrderAsc().stream()
                        .filter(l -> !l.getCode().equals(code) && l.isActive())
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No successor for default"));
                nextDefault.setDefaultLanguage(true);
                siteLanguageRepository.save(nextDefault);
            }
            lang.setActive(false);
        } else {
            lang.setActive(true);
        }
        return toLangDto(siteLanguageRepository.save(lang));
    }

    @Transactional
    public List<LanguageAdminDto> setDefaultLanguage(String rawCode) {
        String code = normalizeCode(rawCode);
        if (!siteLanguageRepository.existsById(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown language: " + code);
        }
        List<SiteLanguage> all = siteLanguageRepository.findAllByOrderBySortOrderAsc();
        for (SiteLanguage l : all) {
            boolean isTarget = l.getCode().equals(code);
            l.setDefaultLanguage(isTarget);
            if (isTarget) {
                l.setActive(true);
            }
        }
        siteLanguageRepository.saveAll(all);
        return listLanguages();
    }

    public List<TranslationEntryDto> getTranslations(String rawLang) {
        String lang = normalizeCode(rawLang);
        assertAllowed(lang);
        return siteTranslationRepository.findByLangCodeOrderByMsgKeyAsc(lang).stream()
                .map(t -> new TranslationEntryDto(t.getMsgKey(), t.getMsgValue()))
                .toList();
    }

    @Transactional
    public void putTranslations(TranslationBulkPutDto dto) {
        String lang = normalizeCode(dto.getLangCode());
        assertAllowed(lang);
        if (dto.getValues() == null || dto.getValues().isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> e : dto.getValues().entrySet()) {
            String key = e.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            if (key.length() > 500) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key too long: " + key.substring(0, 40) + "…");
            }
            String val = e.getValue() != null ? e.getValue() : "";
            SiteTranslation row = siteTranslationRepository.findByLangCodeAndMsgKey(lang, key).orElseGet(() -> {
                SiteTranslation t = new SiteTranslation();
                t.setLangCode(lang);
                t.setMsgKey(key);
                return t;
            });
            row.setMsgValue(val);
            siteTranslationRepository.save(row);
        }
    }

    public static void flattenJson(String prefix, JsonNode node, Map<String, String> out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String p = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenJson(p, entry.getValue(), out);
            });
        } else if (node.isArray()) {
            int i = 0;
            for (JsonNode item : node) {
                flattenJson(prefix + "[" + i + "]", item, out);
                i++;
            }
        } else {
            out.put(prefix, node.asText(""));
        }
    }

    public Map<String, String> readSeedFlat(String lang) throws java.io.IOException {
        var resource = new org.springframework.core.io.ClassPathResource("i18n-seed/" + lang + ".json");
        if (!resource.exists()) {
            return Map.of();
        }
        JsonNode root = objectMapper.readTree(resource.getInputStream());
        Map<String, String> flat = new LinkedHashMap<>();
        flattenJson("", root, flat);
        return flat;
    }

    @Transactional
    public void seedFromClasspathIfEmpty() throws java.io.IOException {
        if (siteLanguageRepository.count() == 0) {
            List<SiteLanguage> langs = new ArrayList<>();
            langs.add(langRow("fr", "Français", "Français", "🇫🇷", true, true, 0));
            langs.add(langRow("en", "English", "English", "🇬🇧", true, false, 1));
            langs.add(langRow("nl", "Nederlands", "Nederlands", "🇳🇱", true, false, 2));
            langs.add(langRow("de", "German", "Deutsch", "🇩🇪", true, false, 3));
            siteLanguageRepository.saveAll(langs);
        }
        if (siteTranslationRepository.count() == 0) {
            for (String lang : ALLOWED_LANG) {
                seedTranslationsForLang(lang);
            }
        }
    }

    /**
     * Adds {@code de} (and empty translation sets) for databases created before German was supported.
     */
    @Transactional
    public void ensureSupportedLanguagesAndMissingTranslations() throws IOException {
        if (!siteLanguageRepository.existsById("de")) {
            siteLanguageRepository.save(langRow("de", "German", "Deutsch", "🇩🇪", true, false, 3));
        }
        for (String lang : ALLOWED_LANG) {
            if (siteTranslationRepository.countByLangCode(lang) == 0) {
                seedTranslationsForLang(lang);
            }
        }
    }

    private void seedTranslationsForLang(String lang) throws IOException {
        Map<String, String> flat = readSeedFlat(lang);
        if (flat.isEmpty()) {
            return;
        }
        List<SiteTranslation> batch = new ArrayList<>();
        for (Map.Entry<String, String> e : flat.entrySet()) {
            SiteTranslation t = new SiteTranslation();
            t.setLangCode(lang);
            t.setMsgKey(e.getKey());
            t.setMsgValue(e.getValue());
            batch.add(t);
            if (batch.size() >= 400) {
                siteTranslationRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            siteTranslationRepository.saveAll(batch);
        }
    }

    private static SiteLanguage langRow(String code, String display, String nativeLbl, String flag, boolean active,
            boolean def, int sort) {
        SiteLanguage l = new SiteLanguage();
        l.setCode(code);
        l.setDisplayName(display);
        l.setNativeLabel(nativeLbl);
        l.setFlagEmoji(flag);
        l.setActive(active);
        l.setDefaultLanguage(def);
        l.setSortOrder(sort);
        return l;
    }

    private LanguageAdminDto toLangDto(SiteLanguage l) {
        return new LanguageAdminDto(l.getCode(), l.getDisplayName(), l.getNativeLabel(), l.getFlagEmoji(), l.isActive(),
                l.isDefaultLanguage());
    }

    private static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lang code required");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static void assertAllowed(String code) {
        if (!ALLOWED_LANG.contains(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language: " + code);
        }
    }
}
