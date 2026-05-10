import { isStaff } from "./roles";

/** Profile URL for the header account menu (clients vs staff). */
export function getProfilePath(roles: string[] | undefined | null): string {
  return isStaff(roles) ? "/backoffice/profile" : "/dashboard/profile";
}
