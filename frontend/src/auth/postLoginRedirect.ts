import { isStaff } from "./roles";

/** Spring Security role strings as returned by the API / JWT. */
export function getPostLoginPath(roles: string[] | undefined | null): string {
  if (isStaff(roles)) return "/backoffice";
  return "/dashboard";
}

/**
 * Client-only redirect target from query string. Rejects open redirects (external URLs, //, etc.).
 * Preserves a safe subset of query params on {@code /dashboard} paths: {@code preselect} and {@code service} (numeric ids only).
 */
export function safeClientRedirectTarget(redirect: string | null | undefined): string | null {
  if (redirect == null || redirect === "") return null;
  const t = redirect.trim();
  if (!t.startsWith("/") || t.startsWith("//")) return null;
  if (t.includes("://") || t.includes("\\")) return null;

  const hashIndex = t.indexOf("#");
  const withoutHash = hashIndex === -1 ? t : t.slice(0, hashIndex);
  const qIndex = withoutHash.indexOf("?");
  const pathOnly = qIndex === -1 ? withoutHash : withoutHash.slice(0, qIndex);
  if (pathOnly.includes("..")) return null;
  if (pathOnly !== "/dashboard" && !pathOnly.startsWith("/dashboard/")) return null;

  if (qIndex === -1) {
    return pathOnly;
  }
  const queryString = withoutHash.slice(qIndex + 1);
  const incoming = new URLSearchParams(queryString);
  const out = new URLSearchParams();
  const pre = incoming.get("preselect");
  const svc = incoming.get("service");
  if (pre && /^\d+$/.test(pre)) {
    out.set("preselect", pre);
  }
  if (svc && /^\d+$/.test(svc)) {
    out.set("service", svc);
  }
  const qs = out.toString();
  return qs ? `${pathOnly}?${qs}` : pathOnly;
}

export function sanitizeServiceIdParam(service: string | null | undefined): string | null {
  if (service == null || service === "") return null;
  const s = service.trim();
  if (!/^\d+$/.test(s)) return null;
  return s;
}

/**
 * Where to send the user after a successful login (or when already authenticated on /login).
 * Staff always go to the back-office; clients may use a whitelisted `redirect` + optional `service` id.
 */
export function buildPostLoginDestination(
  roles: string[] | undefined | null,
  redirect: string | null | undefined,
  service: string | null | undefined
): string {
  if (isStaff(roles)) {
    return getPostLoginPath(roles);
  }
  const path = safeClientRedirectTarget(redirect);
  if (!path) {
    return getPostLoginPath(roles);
  }
  const sid = sanitizeServiceIdParam(service);
  if (!sid) {
    return path;
  }
  if (path.includes("preselect=") || path.includes("service=")) {
    return path;
  }
  const sep = path.includes("?") ? "&" : "?";
  return `${path}${sep}preselect=${encodeURIComponent(sid)}`;
}
