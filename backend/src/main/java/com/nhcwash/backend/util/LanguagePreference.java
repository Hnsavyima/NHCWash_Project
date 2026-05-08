package com.nhcwash.backend.util;

import java.util.Locale;

/**
 * Normalizes UI / API language codes to persisted uppercase codes (FR, EN, NL, DE).
 */
public final class LanguagePreference {

    private LanguagePreference() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "FR";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "FR";
        }
        String u = trimmed.toUpperCase(Locale.ROOT);
        if ("EN".equals(u) || "ENGLISH".equals(u)) {
            return "EN";
        }
        if ("NL".equals(u) || "NL-BE".equals(u) || "NL-NL".equals(u)) {
            return "NL";
        }
        if ("DE".equals(u) || "GERMAN".equals(u) || "DE-DE".equals(u) || "DE-AT".equals(u) || "DE-CH".equals(u)) {
            return "DE";
        }
        if ("FR".equals(u) || "FR-BE".equals(u) || "FR-FR".equals(u)) {
            return "FR";
        }
        String two = u.length() >= 2 ? u.substring(0, 2) : u;
        if ("EN".equals(two)) {
            return "EN";
        }
        if ("NL".equals(two)) {
            return "NL";
        }
        if ("DE".equals(two)) {
            return "DE";
        }
        return "FR";
    }

    /** Thymeleaf template suffix: fr, en, nl, de */
    public static String templateSuffix(String stored) {
        String n = normalize(stored);
        if ("EN".equals(n)) {
            return "en";
        }
        if ("NL".equals(n)) {
            return "nl";
        }
        if ("DE".equals(n)) {
            return "de";
        }
        return "fr";
    }
}
