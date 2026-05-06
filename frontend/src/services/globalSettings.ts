import { apiRequest } from "@/lib/api";
import type { GlobalSettingsDto } from "@/types";

export async function getPublicGlobalSettings(): Promise<GlobalSettingsDto> {
  return apiRequest<GlobalSettingsDto>("/public/settings", { skipAuth: true });
}

export async function getAdminGlobalSettings(): Promise<GlobalSettingsDto> {
  return apiRequest<GlobalSettingsDto>("/admin/settings");
}

export async function updateAdminGlobalSettings(body: GlobalSettingsDto): Promise<GlobalSettingsDto> {
  return apiRequest<GlobalSettingsDto>("/admin/settings", {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
