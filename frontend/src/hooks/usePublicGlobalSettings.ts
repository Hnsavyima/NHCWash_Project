import { useQuery } from "@tanstack/react-query";
import { getPublicGlobalSettings } from "@/services/globalSettings";

export const PUBLIC_GLOBAL_SETTINGS_QUERY_KEY = ["publicGlobalSettings"] as const;

const STALE_MS = 5 * 60 * 1000;

export function usePublicGlobalSettings() {
  return useQuery({
    queryKey: PUBLIC_GLOBAL_SETTINGS_QUERY_KEY,
    queryFn: getPublicGlobalSettings,
    staleTime: STALE_MS,
  });
}
