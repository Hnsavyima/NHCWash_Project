export type UiLangCode = "fr" | "en" | "nl" | "de";

export const PROFILE_LANG_OPTIONS = [
  { code: "fr" as const, labelKey: "profile.langFr" },
  { code: "en" as const, labelKey: "profile.langEn" },
  { code: "nl" as const, labelKey: "profile.langNl" },
  { code: "de" as const, labelKey: "profile.langDe" },
];

export function parsePreferredToUi(raw?: string | null): UiLangCode {
  const p = raw?.trim().slice(0, 2).toLowerCase();
  if (p === "en") return "en";
  if (p === "nl") return "nl";
  if (p === "de") return "de";
  return "fr";
}

export function profileLangToBackend(code: UiLangCode): string {
  if (code === "en") return "EN";
  if (code === "nl") return "NL";
  if (code === "de") return "DE";
  return "FR";
}
