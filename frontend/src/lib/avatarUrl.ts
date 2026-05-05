/**
 * Build an absolute URL for a stored avatar path (e.g. `/uploads/avatars/{uuid}.jpg`).
 */
export function resolveAvatarUrl(avatarUrl: string | null | undefined): string | undefined {
  if (avatarUrl == null || !String(avatarUrl).trim()) return undefined;
  const path = String(avatarUrl).trim();
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  const base = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
  const origin = base.replace(/\/api\/?$/i, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  if (!origin) return normalizedPath;
  return `${origin}${normalizedPath}`;
}
