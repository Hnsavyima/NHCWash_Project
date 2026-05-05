import { AUTH_TOKEN_KEY } from "./authStorage";

const baseUrl = () => (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public body?: unknown
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/** Human-readable fallback when body is empty (common with HTTP/2 + empty statusText). */
function fallbackMessage(status: number, statusText: string): string {
  const st = statusText?.trim();
  if (st) return `${status} ${st}`;
  const map: Record<number, string> = {
    400: "Bad Request",
    401: "Unauthorized — vérifiez email et mot de passe, ou jeton expiré",
    403: "Forbidden — accès refusé (rôle ou ressource)",
    404: "Not Found",
    409: "Conflict",
    422: "Validation error",
    500: "Internal Server Error",
  };
  return map[status] ?? `HTTP ${status}`;
}

export function extractErrorMessage(data: unknown, status: number, statusText: string): string {
  if (typeof data === "string" && data.trim()) {
    const s = data.trim();
    // Avoid dumping full HTML error pages
    if (s.startsWith("<!") || s.startsWith("<html")) {
      return fallbackMessage(status, statusText);
    }
    return s.length > 500 ? `${s.slice(0, 200)}…` : s;
  }
  if (data && typeof data === "object") {
    const o = data as Record<string, unknown>;
    // Spring Boot / ProblemDetail (RFC 7807)
    if (typeof o.detail === "string" && o.detail.trim()) return o.detail;
    if (typeof o.title === "string" && o.title.trim()) return o.title;
    if (typeof o.message === "string" && o.message.trim()) return o.message;
    if (typeof o.error === "string" && o.error.trim()) return o.error;
    // Validation errors
    const errs = o.errors;
    if (Array.isArray(errs) && errs.length > 0) {
      const first = errs[0] as Record<string, unknown>;
      if (typeof first.defaultMessage === "string") return first.defaultMessage as string;
      if (typeof first.message === "string") return first.message as string;
    }
  }
  return fallbackMessage(status, statusText);
}

async function parseBody(res: Response): Promise<unknown> {
  if (res.status === 204) return undefined;
  const len = res.headers.get("content-length");
  if (len === "0") return undefined;
  const text = await res.text();
  if (!text) return undefined;
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) {
    try {
      return JSON.parse(text) as unknown;
    } catch {
      return text;
    }
  }
  return text;
}

export type ApiRequestOptions = RequestInit & { skipAuth?: boolean };

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const base = baseUrl();
  if (!path.startsWith("http") && !base) {
    throw new Error(
      "VITE_API_BASE_URL is not set. Add frontend/.env with VITE_API_BASE_URL=http://localhost:8080/api and restart the Vite dev server."
    );
  }

  const { skipAuth, headers: initHeaders, ...rest } = options;
  const headers = new Headers(initHeaders);

  const hasBody = rest.body !== undefined && rest.body !== null;
  const isFormData = typeof FormData !== "undefined" && rest.body instanceof FormData;
  if (hasBody && typeof rest.body === "string" && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (isFormData) {
    headers.delete("Content-Type");
  }

  if (!skipAuth) {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const url = path.startsWith("http") ? path : `${base}${path.startsWith("/") ? "" : "/"}${path}`;
  const res = await fetch(url, { ...rest, headers });
  const data = await parseBody(res);

  if (!res.ok) {
    const msg = extractErrorMessage(data, res.status, res.statusText);
    throw new ApiError(msg, res.status, data);
  }

  return data as T;
}

/** Authenticated GET returning raw binary (e.g. PDF). */
export async function apiRequestBlob(path: string, options: Omit<ApiRequestOptions, "body"> = {}): Promise<Blob> {
  const base = baseUrl();
  if (!path.startsWith("http") && !base) {
    throw new Error(
      "VITE_API_BASE_URL is not set. Add frontend/.env with VITE_API_BASE_URL=http://localhost:8080/api and restart the Vite dev server."
    );
  }

  const { skipAuth, headers: initHeaders, ...rest } = options;
  const headers = new Headers(initHeaders);

  if (!skipAuth) {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const url = path.startsWith("http") ? path : `${base}${path.startsWith("/") ? "" : "/"}${path}`;
  const res = await fetch(url, { ...rest, headers });

  if (!res.ok) {
    const text = await res.text();
    let data: unknown = text;
    const ct = res.headers.get("content-type") || "";
    if (text && ct.includes("application/json")) {
      try {
        data = JSON.parse(text) as unknown;
      } catch {
        data = text;
      }
    }
    const msg = extractErrorMessage(data, res.status, res.statusText);
    throw new ApiError(msg, res.status, data);
  }

  return res.blob();
}
