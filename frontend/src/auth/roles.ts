/** Matches Spring Security-style (`ROLE_ADMIN`) and plain (`ADMIN`) role strings from the API. */
export type AppRole = "ADMIN" | "EMPLOYEE";

export function normalizeAppRole(role: string): AppRole | null {
  const u = role.toUpperCase().replace(/^ROLE_/, "");
  if (u === "ADMIN") return "ADMIN";
  if (u === "EMPLOYEE") return "EMPLOYEE";
  return null;
}

export function userAppRoles(roles: string[] | undefined | null): Set<AppRole> {
  const set = new Set<AppRole>();
  for (const r of roles ?? []) {
    const n = normalizeAppRole(r);
    if (n) set.add(n);
  }
  return set;
}

export function hasAnyAppRole(
  userRoles: string[] | undefined | null,
  allowed: readonly AppRole[]
): boolean {
  const user = userAppRoles(userRoles);
  return allowed.some((a) => user.has(a));
}

export function isAdmin(userRoles: string[] | undefined | null): boolean {
  return userAppRoles(userRoles).has("ADMIN");
}

export function isStaff(userRoles: string[] | undefined | null): boolean {
  return hasAnyAppRole(userRoles, ["ADMIN", "EMPLOYEE"]);
}
