import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { toast } from "@/components/ui/sonner";
import { useAuth } from "@/auth/AuthContext";
import { applyPreferredLanguage } from "@/i18n/applyPreferredLanguage";
import { patchMyLanguage } from "@/services/users";
import { getPublicSiteLanguages } from "@/services/publicSiteLanguages";
import { ApiError } from "@/lib/api";

const FALLBACK_LANGS = [
  { code: "fr", label: "Français", flag: "🇫🇷" },
  { code: "en", label: "English", flag: "🇬🇧" },
  { code: "nl", label: "Nederlands", flag: "🇳🇱" },
  { code: "de", label: "Deutsch (DE)", flag: "🇩🇪" },
] as const;

const FLAG_BY_CODE: Record<string, string> = {
  fr: "🇫🇷",
  en: "🇬🇧",
  nl: "🇳🇱",
  de: "🇩🇪",
};

function langPrefix(i18nLang: string | undefined): string {
  return (i18nLang ?? "fr").split("-")[0].toLowerCase();
}

function toBackendPreferredLanguage(code: string): string {
  const c = code.toLowerCase();
  if (c === "en") return "EN";
  if (c === "nl") return "NL";
  if (c === "de") return "DE";
  return "FR";
}

const LanguageSwitcher = () => {
  const { t, i18n } = useTranslation();
  const { user, refreshUser } = useAuth();
  const [saving, setSaving] = useState(false);

  const { data: siteLangs } = useQuery({
    queryKey: ["publicSiteLanguages"],
    queryFn: getPublicSiteLanguages,
    staleTime: 60_000,
    retry: 1,
  });

  const languages = useMemo(() => {
    if (siteLangs && siteLangs.length > 0) {
      return siteLangs.map((l) => ({
        code: l.code.toLowerCase(),
        label: l.nativeLabel,
        flag: FLAG_BY_CODE[l.code.toLowerCase()] ?? "🌐",
      }));
    }
    return FALLBACK_LANGS.map((l) => ({ code: l.code, label: l.label, flag: l.flag }));
  }, [siteLangs]);

  const active = langPrefix(i18n.language);
  const current = languages.find((l) => l.code === active) ?? languages[0];

  const handleSelect = async (code: string) => {
    if (code === active || saving) return;
    const previousLng = i18n.language;
    setSaving(true);
    try {
      if (user) {
        await patchMyLanguage(toBackendPreferredLanguage(code));
        await refreshUser();
      }
      await applyPreferredLanguage(i18n, code);
    } catch (e) {
      void i18n.changeLanguage(previousLng);
      const msg =
        e instanceof ApiError ? e.message : e instanceof Error ? e.message : t("auth.errorGeneric");
      toast.error(msg, { position: "bottom-right" });
    } finally {
      setSaving(false);
    }
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          disabled={saving}
          className="max-w-[min(100vw-5rem,14rem)] gap-2 whitespace-normal text-muted-foreground hover:text-foreground sm:max-w-none sm:whitespace-nowrap sm:tabular-nums"
          aria-label={`Language: ${current.label}`}
        >
          <span className="text-base leading-none" aria-hidden>
            {current.flag}
          </span>
          <span className="text-xs font-semibold tracking-wide">{current.code.toUpperCase()}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="max-w-[min(100vw-2rem,18rem)] min-w-[140px]">
        {languages.map((lang) => (
          <DropdownMenuItem
            key={lang.code}
            onClick={() => void handleSelect(lang.code)}
            className={`gap-2 whitespace-normal ${active === lang.code ? "bg-primary/10 text-primary" : ""}`}
            aria-label={lang.label}
          >
            <span className="text-base leading-none" aria-hidden>
              {lang.flag}
            </span>
            <span className="text-xs font-semibold tracking-wide">{lang.code.toUpperCase()}</span>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default LanguageSwitcher;
