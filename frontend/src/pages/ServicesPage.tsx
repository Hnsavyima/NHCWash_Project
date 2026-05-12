import { Link, useNavigate } from "react-router-dom";
import { Shirt, Droplets, Clock, Sparkles, Wind, Scissors, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useTranslation } from "react-i18next";
import { PageSeo } from "@/components/seo/PageSeo";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getServices } from "@/services/services";
import { serviceUnitPrice } from "@/types";
import { tCatalog } from "@/i18n/catalog";
import { useAuth } from "@/auth/AuthContext";
import { CompanyBrandName } from "@/components/CompanyBrandName";

const icons = [Shirt, Droplets, Clock, Sparkles, Wind, Scissors];

const ServicesPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { user, loading: authLoading } = useAuth();
  const [search, setSearch] = useState("");
  const lang = (i18n.language || "fr").slice(0, 2);

  const { data: services = [], isLoading, error } = useQuery({
    queryKey: ["services", lang],
    queryFn: () => getServices(lang),
  });

  const q = search.toLowerCase().trim();
  const goOrderService = (serviceId: number) => {
    if (authLoading) return;
    const target = `/dashboard/new-order?preselect=${serviceId}`;
    if (user) {
      navigate(target);
      return;
    }
    navigate(`/login?redirect=${encodeURIComponent(target)}`);
  };

  const filtered = services.filter((s) => {
    if (!q) return true;
    const nameFr = s.name.toLowerCase();
    const descFr = (s.description || "").toLowerCase();
    const nameT = tCatalog(t, s.name).toLowerCase();
    const descT = tCatalog(t, s.description || "").toLowerCase();
    return nameFr.includes(q) || descFr.includes(q) || nameT.includes(q) || descT.includes(q);
  });

  return (
    <div className="min-h-screen bg-background">
      <PageSeo titleKey="seo.services.title" descriptionKey="seo.services.description" />
      <header className="sticky top-0 z-50 glass">
        <div className="container mx-auto flex items-center justify-between px-4 py-4">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg gradient-hero">
              <Droplets className="h-5 w-5 text-primary-foreground" />
            </div>
            <CompanyBrandName className="font-display text-xl font-bold text-foreground" />
          </Link>
          <div className="flex items-center gap-3">
            <LanguageSwitcher />
            <Link to="/login">
              <Button variant="ghost" size="sm">
                {t("nav.login")}
              </Button>
            </Link>
            <Link to="/register">
              <Button size="sm">{t("nav.register")}</Button>
            </Link>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-12">
        <div className="mb-8">
          <h1 className="font-display text-3xl font-bold text-foreground">{t("services.title")}</h1>
          <p className="mt-2 text-muted-foreground">{t("services.subtitle")}</p>
        </div>

        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="relative max-w-sm flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t("services.searchPlaceholder")}
              className="pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {isLoading && (
          <p className="text-sm text-muted-foreground">{t("services.loading", { defaultValue: "Chargement…" })}</p>
        )}
        {error && (
          <p className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {(error as Error).message}
          </p>
        )}

        {!isLoading && !error && filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-border bg-card p-12 text-center">
            <Search className="h-12 w-12 text-muted-foreground/50" />
            <h3 className="mt-4 font-display text-lg font-semibold text-foreground">{t("services.noResults")}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{t("services.noResultsSub")}</p>
          </div>
        ) : (
          !isLoading &&
          !error && (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {filtered.map((svc, idx) => {
                const Icon = icons[idx % icons.length];
                const price = serviceUnitPrice(svc);
                return (
                  <div
                    key={svc.id}
                    className="group rounded-xl border border-border bg-card p-6 shadow-card transition-all hover:-translate-y-1 hover:shadow-card-hover"
                  >
                    <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                      <Icon className="h-6 w-6 text-primary" />
                    </div>
                    <h3 className="mt-4 font-display text-lg font-semibold text-foreground">{tCatalog(t, svc.name)}</h3>
                    <p className="mt-2 text-sm text-muted-foreground">{tCatalog(t, svc.description)}</p>
                    <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
                      <div>
                        <span className="text-lg font-bold text-foreground">{price.toFixed(2)}€</span>
                        <span className="text-sm text-muted-foreground"> {t("services.perPiece")}</span>
                      </div>
                    </div>
                    <Button
                      type="button"
                      className="mt-4 w-full"
                      variant="outline"
                      disabled={authLoading}
                      onClick={() => goOrderService(svc.id)}
                    >
                      {t("services.order")}
                    </Button>
                  </div>
                );
              })}
            </div>
          )
        )}
      </main>
    </div>
  );
};

export default ServicesPage;
