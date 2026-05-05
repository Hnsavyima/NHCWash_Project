import type { TFunction } from "i18next";

/**
 * Catalogue strings from the DB are stored in French. Use the French text as i18n key
 * so en/nl (and future locales) can translate while fr falls back to the key.
 */
export function tCatalog(t: TFunction, value: string | null | undefined): string {
  if (value == null || String(value).trim() === "") return "";
  const v = String(value);
  return String(t(v, { defaultValue: v }));
}
