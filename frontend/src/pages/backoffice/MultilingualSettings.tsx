import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Globe, Star } from "lucide-react";
import BackOfficeLayout from "@/components/layouts/BackOfficeLayout";
import { DataFetchState } from "@/components/backoffice/DataFetchState";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ApiError } from "@/lib/api";
import {
  getAdminLanguages,
  getAdminTranslations,
  putAdminTranslations,
  setDefaultAdminLanguage,
  toggleAdminLanguage,
  type LanguageAdminDto,
} from "@/services/adminI18n";
import { toast } from "@/components/ui/sonner";
import { Pagination } from "@/components/ui/pagination";

const LANG_QUERY = ["admin", "i18n", "languages"] as const;
const transQueryKey = (code: string) => ["admin", "i18n", "translations", code] as const;

export default function MultilingualSettings() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const {
    data: languages = [],
    isPending: langsLoading,
    isError: langsError,
    error: langsQueryError,
  } = useQuery({
    queryKey: LANG_QUERY,
    queryFn: getAdminLanguages,
  });

  const [editLang, setEditLang] = useState<string>("fr");
  const [search, setSearch] = useState("");
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    if (!languages.length) return;
    setEditLang((prev) => {
      if (languages.some((l) => l.code === prev)) return prev;
      const def = languages.find((l) => l.defaultLanguage);
      return def?.code ?? languages[0].code;
    });
  }, [languages]);

  const {
    data: entries = [],
    isPending: transLoading,
    isError: transError,
    error: transQueryError,
  } = useQuery({
    queryKey: transQueryKey(editLang),
    queryFn: () => getAdminTranslations(editLang),
    enabled: Boolean(editLang),
  });

  useEffect(() => {
    const next: Record<string, string> = {};
    for (const e of entries) {
      next[e.key] = e.value ?? "";
    }
    setDraft(next);
  }, [entries, editLang]);

  const langsErr =
    langsError && langsQueryError
      ? langsQueryError instanceof ApiError
        ? langsQueryError.message
        : t("backoffice.i18nManager.loadLanguagesError")
      : null;

  const transErr =
    transError && transQueryError
      ? transQueryError instanceof ApiError
        ? transQueryError.message
        : t("backoffice.i18nManager.loadTranslationsError")
      : null;

  const filteredKeys = useMemo(() => {
    const q = search.trim().toLowerCase();
    const keys = Object.keys(draft).sort((a, b) => a.localeCompare(b));
    if (!q) return keys;
    return keys.filter((k) => k.toLowerCase().includes(q));
  }, [draft, search]);

  const sourceData = filteredKeys;

  useEffect(() => {
    setCurrentPage(1);
  }, [search, editLang]);

  const ITEMS_PER_PAGE = 10;
  const totalPages = Math.ceil(sourceData.length / ITEMS_PER_PAGE);
  const page = Math.max(1, Math.min(currentPage, totalPages || 1));
  const paginatedData = sourceData.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

  const toggleMutation = useMutation({
    mutationFn: (code: string) => toggleAdminLanguage(code),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: LANG_QUERY });
      void queryClient.invalidateQueries({ queryKey: ["publicSiteLanguages"] });
    },
    onError: (e: unknown) => {
      toast.error(e instanceof ApiError ? e.message : t("backoffice.i18nManager.toggleError"));
    },
  });

  const defaultMutation = useMutation({
    mutationFn: (code: string) => setDefaultAdminLanguage(code),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: LANG_QUERY });
      void queryClient.invalidateQueries({ queryKey: ["publicSiteLanguages"] });
    },
    onError: (e: unknown) => {
      toast.error(e instanceof ApiError ? e.message : t("backoffice.i18nManager.setDefaultError"));
    },
  });

  const saveMutation = useMutation({
    mutationFn: () => putAdminTranslations(editLang, draft),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: transQueryKey(editLang) });
      toast.success(t("backoffice.i18nManager.saveSuccess"));
    },
    onError: (e: unknown) => {
      toast.error(e instanceof ApiError ? e.message : t("backoffice.i18nManager.saveError"));
    },
  });

  const onToggle = (lang: LanguageAdminDto, checked: boolean) => {
    if (checked === lang.active) return;
    toggleMutation.mutate(lang.code);
  };

  const editingMeta = languages.find((l) => l.code === editLang);

  return (
    <BackOfficeLayout>
      <div className="space-y-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <div className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Globe className="h-5 w-5" aria-hidden />
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold text-foreground">
                {t("backoffice.i18nManager.pageTitle")}
              </h1>
              <p className="text-sm text-muted-foreground">{t("backoffice.i18nManager.pageSubtitle")}</p>
            </div>
          </div>
        </div>

        <section className="space-y-3">
          <h2 className="text-lg font-semibold tracking-tight">{t("backoffice.i18nManager.sectionLanguages")}</h2>
          <DataFetchState loading={langsLoading} error={langsErr} empty={!langsLoading && !langsErr && languages.length === 0}>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {languages.map((lang) => (
                <div
                  key={lang.code}
                  className="flex flex-col gap-4 rounded-xl border border-border bg-card p-5 shadow-card"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="text-3xl leading-none" aria-hidden>
                        {lang.flagEmoji}
                      </span>
                      <div className="min-w-0">
                        <p className="font-semibold text-foreground truncate">{lang.displayName}</p>
                        <p className="text-xs text-muted-foreground">{lang.nativeLabel}</p>
                      </div>
                    </div>
                    {lang.defaultLanguage ? (
                      <Badge className="shrink-0 bg-primary text-primary-foreground hover:bg-primary">
                        {t("backoffice.i18nManager.defaultBadge")}
                      </Badge>
                    ) : null}
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4">
                    <div className="flex items-center gap-2">
                      <Switch
                        id={`lang-active-${lang.code}`}
                        checked={lang.active}
                        disabled={toggleMutation.isPending}
                        onCheckedChange={(c) => onToggle(lang, c)}
                        aria-label={t("backoffice.i18nManager.activeLabel")}
                      />
                      <label htmlFor={`lang-active-${lang.code}`} className="text-sm text-muted-foreground cursor-pointer">
                        {t("backoffice.i18nManager.activeLabel")}
                      </label>
                    </div>
                    {!lang.defaultLanguage ? (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="gap-1"
                        disabled={defaultMutation.isPending}
                        onClick={() => defaultMutation.mutate(lang.code)}
                      >
                        <Star className="h-3.5 w-3.5" aria-hidden />
                        {t("backoffice.i18nManager.setDefault")}
                      </Button>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          </DataFetchState>
        </section>

        <section className="space-y-3">
          <h2 className="text-lg font-semibold tracking-tight">{t("backoffice.i18nManager.sectionEditor")}</h2>
          <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
            <div className="flex flex-col gap-4 border-b border-border bg-muted/30 p-4 sm:flex-row sm:flex-wrap sm:items-end sm:justify-between">
              <div className="space-y-2 min-w-[200px]">
                <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  {t("backoffice.i18nManager.editorLang")}
                </p>
                <Select value={editLang} onValueChange={setEditLang} disabled={langsLoading || languages.length === 0}>
                  <SelectTrigger className="w-full sm:w-[260px] bg-background">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {languages.map((l) => (
                      <SelectItem key={l.code} value={l.code}>
                        <span className="mr-2" aria-hidden>
                          {l.flagEmoji}
                        </span>
                        {l.nativeLabel}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {editingMeta ? (
                  <p className="text-sm text-muted-foreground">
                    {t("backoffice.i18nManager.editingLabel", { lang: editingMeta.nativeLabel })}
                  </p>
                ) : null}
              </div>
              <div className="flex flex-1 flex-col gap-2 sm:max-w-md sm:flex-1">
                <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  {t("backoffice.i18nManager.searchLabel")}
                </p>
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder={t("backoffice.i18nManager.searchPlaceholder")}
                  className="bg-background"
                />
              </div>
              <Button
                type="button"
                className="gradient-hero border-0 text-primary-foreground hover:opacity-90 sm:self-end"
                disabled={saveMutation.isPending || transLoading || Boolean(transErr)}
                onClick={() => saveMutation.mutate()}
              >
                {t("backoffice.i18nManager.saveButton")}
              </Button>
            </div>

            <DataFetchState loading={transLoading} error={transErr} empty={!transLoading && !transErr && entries.length === 0}>
              <p className="px-4 py-2 text-xs text-muted-foreground border-b border-border">
                {t("backoffice.i18nManager.keysLine", {
                  filtered: filteredKeys.length,
                  total: Object.keys(draft).length,
                })}
              </p>
              <div className="max-h-[min(60vh,520px)] overflow-auto">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead className="sticky top-0 z-10 border-b border-border bg-card">
                    <tr className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                      <th className="w-[38%] px-4 py-3 align-middle">{t("backoffice.i18nManager.colKey")}</th>
                      <th className="px-4 py-3 align-middle">{t("backoffice.i18nManager.colValue")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedData.map((key) => (
                      <tr key={key} className="border-b border-border/80 last:border-0">
                        <td className="px-4 py-2 align-top font-mono text-xs text-muted-foreground break-all">{key}</td>
                        <td className="px-4 py-2 align-top">
                          <Input
                            value={draft[key] ?? ""}
                            onChange={(e) => setDraft((d) => ({ ...d, [key]: e.target.value }))}
                            className="bg-background font-normal"
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {!transLoading && !transErr && entries.length > 0 && totalPages > 1 ? (
                <div className="mt-4 border-t border-gray-100 px-4 pt-4 pb-2 dark:border-gray-800">
                  <Pagination
                    className="border-t-0 bg-transparent"
                    currentPage={page}
                    totalPages={totalPages}
                    onPageChange={setCurrentPage}
                  />
                </div>
              ) : null}
            </DataFetchState>
          </div>
        </section>
      </div>
    </BackOfficeLayout>
  );
}
