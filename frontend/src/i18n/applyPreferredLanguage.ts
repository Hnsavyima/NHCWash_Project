import type { i18n as I18nApi } from "i18next";

/** Default key used by i18next-browser-languagedetector with `caches: ['localStorage']`. */
const I18NEXT_LNG_STORAGE_KEY = "i18nextLng";

/**
 * Applies the user's persisted locale to i18n and mirrors it to localStorage
 * so the detector stays aligned with the server on the next visit (FR / EN / NL / DE).
 */
export async function applyPreferredLanguage(i18n: I18nApi, raw?: string | null): Promise<void> {
  const p = raw?.trim().slice(0, 2).toLowerCase();
  if (p !== "fr" && p !== "en" && p !== "nl" && p !== "de") {
    return;
  }
  await i18n.changeLanguage(p);
  try {
    window.localStorage?.setItem(I18NEXT_LNG_STORAGE_KEY, p);
  } catch {
    /* private mode / blocked storage */
  }
}
