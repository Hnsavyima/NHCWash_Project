import { usePublicGlobalSettings } from "@/hooks/usePublicGlobalSettings";

const FALLBACK = "NHCWash";

type Props = {
  className?: string;
};

/** Visible company name from public settings (falls back while loading). */
export function CompanyBrandName({ className }: Props) {
  const { data } = usePublicGlobalSettings();
  const name = data?.companyName?.trim() || FALLBACK;
  return <span className={className}>{name}</span>;
}
