import { apiRequest } from "@/lib/api";

export type PublicSiteLanguageDto = {
  code: string;
  nativeLabel: string;
};

export function getPublicSiteLanguages(): Promise<PublicSiteLanguageDto[]> {
  return apiRequest<PublicSiteLanguageDto[]>("/public/site-languages", { skipAuth: true });
}
