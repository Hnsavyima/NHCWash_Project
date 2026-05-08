import { apiRequest } from "@/lib/api";

export type LanguageAdminDto = {
  code: string;
  displayName: string;
  nativeLabel: string;
  flagEmoji: string;
  active: boolean;
  defaultLanguage: boolean;
};

export type TranslationEntryDto = {
  key: string;
  value: string;
};

export function getAdminLanguages(): Promise<LanguageAdminDto[]> {
  return apiRequest<LanguageAdminDto[]>("/admin/languages");
}

export function toggleAdminLanguage(code: string): Promise<LanguageAdminDto> {
  return apiRequest<LanguageAdminDto>(`/admin/languages/${encodeURIComponent(code)}/toggle`, {
    method: "PUT",
  });
}

export function setDefaultAdminLanguage(code: string): Promise<LanguageAdminDto[]> {
  return apiRequest<LanguageAdminDto[]>(`/admin/languages/${encodeURIComponent(code)}/default`, {
    method: "PUT",
  });
}

export function getAdminTranslations(langCode: string): Promise<TranslationEntryDto[]> {
  return apiRequest<TranslationEntryDto[]>(`/admin/translations/${encodeURIComponent(langCode)}`);
}

export function putAdminTranslations(langCode: string, values: Record<string, string>): Promise<void> {
  return apiRequest<void>("/admin/translations", {
    method: "PUT",
    body: JSON.stringify({ langCode, values }),
  });
}
