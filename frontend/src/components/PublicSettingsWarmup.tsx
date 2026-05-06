import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { PUBLIC_GLOBAL_SETTINGS_QUERY_KEY } from "@/hooks/usePublicGlobalSettings";
import { getPublicGlobalSettings } from "@/services/globalSettings";

/** Prefetch public site settings for faster first paint (footer, headers, legal). */
export function PublicSettingsWarmup() {
  const queryClient = useQueryClient();
  useEffect(() => {
    void queryClient.prefetchQuery({
      queryKey: PUBLIC_GLOBAL_SETTINGS_QUERY_KEY,
      queryFn: getPublicGlobalSettings,
      staleTime: 5 * 60 * 1000,
    });
  }, [queryClient]);
  return null;
}
