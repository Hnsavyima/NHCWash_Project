import { apiRequest, ApiError } from "@/lib/api";

export interface LoginResponse {
  token: string;
  email: string;
  roles: string[];
  preferredLanguage?: string | null;
}

export interface RegisterPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  /** Required at signup (delivery contact). */
  phone: string;
  /** FR, EN, or NL — sent to backend on registration */
  preferredLanguage?: string;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const data = await apiRequest<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
    skipAuth: true,
  });
  if (!data?.token || typeof data.token !== "string" || !data.token.trim()) {
    throw new ApiError(
      "Réponse serveur invalide : champ « token » manquant (vérifiez JwtResponse côté API)",
      500,
      data
    );
  }
  return {
    token: data.token.trim(),
    email: data.email ?? "",
    roles: Array.isArray(data.roles) ? data.roles : [],
    preferredLanguage: data.preferredLanguage ?? null,
  };
}

/** Returns plain text success message from backend */
export async function register(payload: RegisterPayload): Promise<string> {
  return apiRequest<string>("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
    skipAuth: true,
  });
}

/**
 * Multipart registration. The photo is optional: when omitted or null, only text fields are sent
 * (still as `multipart/form-data`, which matches the backend).
 */
export async function registerWithAvatar(payload: RegisterPayload, avatar?: File | null): Promise<string> {
  const fd = new FormData();
  fd.append("email", payload.email.trim());
  fd.append("password", payload.password);
  fd.append("firstName", payload.firstName.trim());
  fd.append("lastName", payload.lastName.trim());
  fd.append("phone", payload.phone.trim());
  if (avatar && avatar.size > 0) {
    fd.append("avatar", avatar);
  }
  if (payload.preferredLanguage != null && payload.preferredLanguage.trim() !== "") {
    fd.append("preferredLanguage", payload.preferredLanguage.trim().toUpperCase());
  }
  return apiRequest<string>("/auth/register", {
    method: "POST",
    body: fd,
    skipAuth: true,
  });
}

export async function requestPasswordReset(email: string): Promise<void> {
  await apiRequest<void>("/auth/password-reset/request", {
    method: "POST",
    body: JSON.stringify({ email: email.trim() }),
    skipAuth: true,
  });
}

export async function confirmPasswordReset(token: string, newPassword: string): Promise<void> {
  await apiRequest<void>("/auth/password-reset/confirm", {
    method: "POST",
    body: JSON.stringify({ token: token.trim(), newPassword }),
    skipAuth: true,
  });
}
