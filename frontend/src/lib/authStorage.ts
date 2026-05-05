/** localStorage keys for JWT session */
export const AUTH_TOKEN_KEY = "nhcwash_token";
export const AUTH_EMAIL_KEY = "nhcwash_email";
export const AUTH_ROLES_KEY = "nhcwash_roles";

export function getStoredToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_KEY);
}

export function clearAuthStorage(): void {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_EMAIL_KEY);
  localStorage.removeItem(AUTH_ROLES_KEY);
}

export function persistAuthSession(token: string, email: string, roles: string[]): void {
  localStorage.setItem(AUTH_TOKEN_KEY, token);
  localStorage.setItem(AUTH_EMAIL_KEY, email);
  localStorage.setItem(AUTH_ROLES_KEY, JSON.stringify(roles));
}
