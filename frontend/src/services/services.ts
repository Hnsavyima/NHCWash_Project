import { apiRequest } from "@/lib/api";
import type { ServiceDto } from "@/types";

export async function getServices(lang = "fr"): Promise<ServiceDto[]> {
  const q = new URLSearchParams({ lang });
  return apiRequest<ServiceDto[]>(`/services?${q.toString()}`);
}
