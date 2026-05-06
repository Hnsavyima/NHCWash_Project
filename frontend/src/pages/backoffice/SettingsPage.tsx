import { useEffect, useState } from "react";
import BackOfficeLayout from "@/components/layouts/BackOfficeLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Save } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getAdminGlobalSettings, updateAdminGlobalSettings } from "@/services/globalSettings";
import { PUBLIC_GLOBAL_SETTINGS_QUERY_KEY } from "@/hooks/usePublicGlobalSettings";
import type { GlobalSettingsDto } from "@/types";
import { toast } from "@/components/ui/sonner";
import { ApiError } from "@/lib/api";

const ADMIN_SETTINGS_QUERY_KEY = ["admin", "globalSettings"] as const;

export default function SettingsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<GlobalSettingsDto | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ADMIN_SETTINGS_QUERY_KEY,
    queryFn: getAdminGlobalSettings,
  });

  useEffect(() => {
    if (data) {
      setForm({ ...data });
    }
  }, [data]);

  const saveMutation = useMutation({
    mutationFn: updateAdminGlobalSettings,
    onSuccess: (saved) => {
      setForm({ ...saved });
      void queryClient.invalidateQueries({ queryKey: ADMIN_SETTINGS_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: PUBLIC_GLOBAL_SETTINGS_QUERY_KEY });
      toast.success(t("backoffice.globalSettings.saveSuccess"));
    },
    onError: (e) => {
      toast.error(e instanceof ApiError ? e.message : t("backoffice.globalSettings.saveError"));
    },
  });

  const handleChange = (field: keyof GlobalSettingsDto, value: string) => {
    setForm((prev) => (prev ? { ...prev, [field]: value } : prev));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form || saveMutation.isPending) return;
    saveMutation.mutate(form);
  };

  if (error) {
    return (
      <BackOfficeLayout>
        <p className="text-sm text-destructive">{t("backoffice.globalSettings.loadError")}</p>
      </BackOfficeLayout>
    );
  }

  return (
    <BackOfficeLayout>
      <div className="space-y-6 pb-24">
        <div>
          <h1 className="font-display text-2xl font-bold text-foreground">{t("backoffice.globalSettings.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("backoffice.globalSettings.subtitle")}</p>
        </div>

        {isLoading || !form ? (
          <div className="py-12 text-center text-muted-foreground">…</div>
        ) : (
          <form onSubmit={(e) => void handleSubmit(e)} className="space-y-8">
            <section className="rounded-xl border border-border bg-card p-6 shadow-card">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("backoffice.settingsForm.identitySection")}</h2>
              <div className="mt-4 space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="gs-company">{t("backoffice.settingsForm.companyName")}</Label>
                  <Input
                    id="gs-company"
                    value={form.companyName}
                    onChange={(e) => handleChange("companyName", e.target.value)}
                    required
                    maxLength={200}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="gs-hours">{t("backoffice.settingsForm.openingHours")}</Label>
                  <p className="text-xs text-muted-foreground">{t("backoffice.settingsForm.openingHoursHint")}</p>
                  <Textarea
                    id="gs-hours"
                    value={form.openingHoursDescription}
                    onChange={(e) => handleChange("openingHoursDescription", e.target.value)}
                    rows={5}
                    className="min-h-[120px] resize-y"
                  />
                </div>
              </div>
            </section>

            <section className="rounded-xl border border-border bg-card p-6 shadow-card">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("backoffice.settingsForm.contactSection")}</h2>
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="gs-contact-email">{t("backoffice.settingsForm.contactEmail")}</Label>
                  <Input
                    id="gs-contact-email"
                    type="email"
                    value={form.contactEmail}
                    onChange={(e) => handleChange("contactEmail", e.target.value)}
                    required
                    maxLength={255}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="gs-phone">{t("backoffice.settingsForm.contactPhone")}</Label>
                  <Input
                    id="gs-phone"
                    value={form.contactPhone}
                    onChange={(e) => handleChange("contactPhone", e.target.value)}
                    required
                    maxLength={80}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="gs-support">{t("backoffice.settingsForm.supportEmail")}</Label>
                  <Input
                    id="gs-support"
                    type="email"
                    value={form.supportEmail}
                    onChange={(e) => handleChange("supportEmail", e.target.value)}
                    required
                    maxLength={255}
                  />
                </div>
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="gs-address">{t("backoffice.settingsForm.address")}</Label>
                  <Textarea
                    id="gs-address"
                    value={form.address}
                    onChange={(e) => handleChange("address", e.target.value)}
                    required
                    rows={3}
                    maxLength={1000}
                    className="resize-y"
                  />
                </div>
              </div>
            </section>

            <section className="rounded-xl border border-border bg-card p-6 shadow-card">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("backoffice.settingsForm.legalSection")}</h2>
              <div className="mt-4 space-y-2">
                <Label htmlFor="gs-vat">{t("backoffice.settingsForm.vatNumber")}</Label>
                <Input
                  id="gs-vat"
                  value={form.vatNumber}
                  onChange={(e) => handleChange("vatNumber", e.target.value)}
                  required
                  maxLength={120}
                />
              </div>
            </section>

            <div className="sticky bottom-0 z-10 -mx-2 flex justify-end rounded-xl border border-border bg-card/95 px-4 py-4 shadow-lg backdrop-blur supports-[backdrop-filter]:bg-card/80">
              <Button
                type="submit"
                className="gradient-hero border-0 text-primary-foreground hover:opacity-90"
                size="lg"
                disabled={saveMutation.isPending}
              >
                <Save className="mr-2 h-4 w-4" />
                {t("backoffice.globalSettings.saveButton")}
              </Button>
            </div>
          </form>
        )}
      </div>
    </BackOfficeLayout>
  );
}
