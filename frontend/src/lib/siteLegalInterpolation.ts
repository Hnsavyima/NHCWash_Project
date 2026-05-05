import type { GlobalSettingsDto } from "@/types";

const FALLBACK_COMPANY = "NHCWash";

/** Values for `t(..., { ... })` on legal pages and marketing copy. */
export function siteLegalInterpolation(site: GlobalSettingsDto | undefined) {
  const companyName = site?.companyName?.trim() || FALLBACK_COMPANY;
  const address = site?.address?.trim() || "";
  const contactEmail = site?.contactEmail?.trim() || "";
  const supportEmail = (site?.supportEmail?.trim() || contactEmail).trim();
  return { companyName, address, contactEmail, supportEmail };
}
