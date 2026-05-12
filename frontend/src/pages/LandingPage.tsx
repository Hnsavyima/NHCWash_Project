import { Link } from "react-router-dom";
import { Shirt, Droplets, Clock, ArrowRight, CheckCircle, Phone, Mail, MapPin, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useTranslation } from "react-i18next";
import { PageSeo } from "@/components/seo/PageSeo";
import { useQuery } from "@tanstack/react-query";
import heroImage from "@/assets/hero-laundry.jpg";
import { getServices } from "@/services/services";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { CompanyBrandName } from "@/components/CompanyBrandName";
import { usePublicGlobalSettings } from "@/hooks/usePublicGlobalSettings";
import { siteLegalInterpolation } from "@/lib/siteLegalInterpolation";
import { serviceUnitPrice } from "@/types";
import { tCatalog } from "@/i18n/catalog";

const serviceIcons = [Shirt, Droplets, Clock];

const LandingPage = () => {
  const { t, i18n } = useTranslation();
  const lang = (i18n.language || "fr").slice(0, 2);
  const { data: services = [] } = useQuery({
    queryKey: ["services", lang],
    queryFn: () => getServices(lang),
  });
  const { data: site } = usePublicGlobalSettings();
  const legal = siteLegalInterpolation(site);
  const popularServices = services.slice(0, 3);

  const steps = [
    { number: "01", title: t('landing.step1Title'), description: t('landing.step1Desc') },
    { number: "02", title: t('landing.step2Title'), description: t('landing.step2Desc') },
    { number: "03", title: t('landing.step3Title'), description: t('landing.step3Desc') },
  ];

  return (
    <div className="min-h-screen bg-background">
      <PageSeo titleKey="seo.landing.title" descriptionKey="seo.landing.description" />
      {/* Header */}
      <header className="sticky top-0 z-50 glass">
        <div className="container mx-auto flex items-center justify-between px-4 py-4">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg gradient-hero">
              <Droplets className="h-5 w-5 text-primary-foreground" />
            </div>
            <CompanyBrandName className="font-display text-xl font-bold text-foreground" />
          </Link>
          <nav className="hidden items-center gap-8 md:flex">
            <Link to="/services" className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">{t('nav.services')}</Link>
            <Link to="/contact" className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">{t('nav.contact')}</Link>
          </nav>
          <div className="flex items-center gap-3">
            <LanguageSwitcher />
            <Link to="/login">
              <Button variant="ghost" size="sm">{t('nav.login')}</Button>
            </Link>
            <Link to="/register">
              <Button size="sm">{t('nav.register')}</Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden py-20 lg:py-32">
        <div className="absolute inset-0 gradient-surface" />
        <div className="container relative mx-auto px-4">
          <div className="grid items-center gap-12 lg:grid-cols-2">
            <div className="animate-fade-up">
              <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-accent/10 px-4 py-1.5 text-sm font-medium text-accent">
                <Star className="h-4 w-4" /> {t('landing.badge')}
              </div>
              <h1 className="font-display text-4xl font-extrabold leading-tight text-foreground sm:text-5xl lg:text-6xl">
                {t('landing.heroTitle1')}{" "}
                <span className="text-gradient">{t('landing.heroTitle2')}</span>
                <br />{t('landing.heroTitle3')}
              </h1>
              <p className="mt-6 max-w-lg text-lg text-muted-foreground">
                {t('landing.heroDescription')}
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <Link to="/login">
                  <Button size="lg" className="gap-2 gradient-hero border-0 text-primary-foreground shadow-lg hover:opacity-90">
                    {t('landing.ctaOrder')} <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
                <Link to="/services">
                  <Button size="lg" variant="outline">
                    {t('landing.ctaServices')}
                  </Button>
                </Link>
              </div>
              <div className="mt-8 flex items-center gap-6 text-sm text-muted-foreground">
                <span className="flex items-center gap-1.5"><CheckCircle className="h-4 w-4 text-accent" /> {t('landing.freePickup')}</span>
                <span className="flex items-center gap-1.5"><CheckCircle className="h-4 w-4 text-accent" /> {t('landing.delivery24h')}</span>
                <span className="flex items-center gap-1.5"><CheckCircle className="h-4 w-4 text-accent" /> {t('landing.securePayment')}</span>
              </div>
            </div>
            <div className="animate-fade-up relative hidden lg:block" style={{ animationDelay: "0.2s" }}>
              <div className="overflow-hidden rounded-2xl shadow-xl">
                <img
                  src={heroImage}
                  alt={t("landing.heroImageAlt")}
                  className="h-full w-full object-cover"
                  loading="lazy"
                  decoding="async"
                />
              </div>
              <div className="absolute -bottom-4 -left-4 rounded-xl bg-card p-4 shadow-card-hover">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-accent/10">
                    <CheckCircle className="h-5 w-5 text-accent" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">{t('landing.ordersDelivered')}</p>
                    <p className="text-xs text-muted-foreground">{t('landing.ordersDeliveredSub')}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Services */}
      <section className="py-20">
        <div className="container mx-auto px-4">
          <div className="mb-12 text-center">
            <h2 className="font-display text-3xl font-bold text-foreground">{t('landing.popularServices')}</h2>
            <p className="mt-3 text-muted-foreground">{t('landing.popularServicesSub')}</p>
          </div>
          <div className="grid gap-6 md:grid-cols-3">
            {popularServices.map((s, idx) => {
              const Icon = serviceIcons[idx % serviceIcons.length];
              const price = serviceUnitPrice(s);
              return (
                <Link
                  key={s.id}
                  to={`/login?${new URLSearchParams({
                    redirect: `/dashboard/new-order?preselect=${s.id}`,
                  }).toString()}`}
                  className="group block cursor-pointer select-none rounded-xl border border-border bg-card p-6 shadow-card outline-none transition-all duration-200 hover:-translate-y-1 hover:border-primary hover:shadow-md focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-background"
                  aria-label={`${t("nav.login")} — ${tCatalog(t, s.name)}`}
                >
                  <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 transition-colors group-hover:bg-primary/15">
                    <Icon className="h-6 w-6 text-primary" aria-hidden />
                  </div>
                  <h3 className="font-display text-lg font-semibold text-foreground">{tCatalog(t, s.name)}</h3>
                  <p className="mt-2 text-sm text-muted-foreground">{tCatalog(t, s.description)}</p>
                  <div className="mt-4 flex items-center justify-between border-t border-border pt-4 text-sm">
                    <span className="font-semibold text-foreground">
                      {t("landing.priceFrom")} {price.toFixed(2)}€{t("landing.perPiece")}
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="gradient-surface py-20">
        <div className="container mx-auto px-4">
          <div className="mb-12 text-center">
            <h2 className="font-display text-3xl font-bold text-foreground">{t('landing.howItWorks')}</h2>
            <p className="mt-3 text-muted-foreground">{t('landing.howItWorksSub')}</p>
          </div>
          <div className="grid gap-8 md:grid-cols-3">
            {steps.map((step, i) => (
              <div key={step.number} className="relative text-center animate-fade-up" style={{ animationDelay: `${i * 0.15}s` }}>
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full gradient-hero text-2xl font-bold text-primary-foreground font-display">
                  {step.number}
                </div>
                <h3 className="font-display text-lg font-semibold text-foreground">{step.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{step.description}</p>
              </div>
            ))}
          </div>
          <div className="mt-12 text-center">
            <Link to="/login">
              <Button size="lg" className="gap-2 gradient-hero border-0 text-primary-foreground hover:opacity-90">
                {t('landing.startNow')} <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border bg-card py-12">
        <div className="container mx-auto px-4">
          <div className="grid gap-8 md:grid-cols-4">
            <div>
              <div className="flex items-center gap-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg gradient-hero">
                  <Droplets className="h-4 w-4 text-primary-foreground" />
                </div>
                <CompanyBrandName className="font-display text-lg font-bold text-foreground" />
              </div>
              <p className="mt-3 text-sm text-muted-foreground">{t('landing.footerTagline')}</p>
            </div>
            <div>
              <h4 className="font-display text-sm font-semibold text-foreground">{t('landing.navigation')}</h4>
              <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
                <li><Link to="/" className="hover:text-foreground">{t('landing.home')}</Link></li>
                <li><Link to="/services" className="hover:text-foreground">{t('nav.services')}</Link></li>
                <li><Link to="/contact" className="hover:text-foreground">{t('nav.contact')}</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="font-display text-sm font-semibold text-foreground">{t('landing.legal')}</h4>
              <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link to="/mentions-legales" className="transition-colors hover:text-foreground hover:underline">
                    {t("landing.legalNotice")}
                  </Link>
                </li>
                <li>
                  <Link
                    to="/politique-confidentialite"
                    className="transition-colors hover:text-foreground hover:underline"
                  >
                    {t("landing.privacyPolicy")}
                  </Link>
                </li>
                <li>
                  <Link to="/rgpd" className="transition-colors hover:text-foreground hover:underline">
                    {t("landing.gdpr")}
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-display text-sm font-semibold text-foreground">{t('landing.contactTitle')}</h4>
              <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
                <li className="flex items-center gap-2">
                  <Phone className="h-4 w-4" /> {site?.contactPhone?.trim() || "—"}
                </li>
                <li className="flex items-center gap-2">
                  <Mail className="h-4 w-4 shrink-0" />
                  {site?.contactEmail?.trim() ? (
                    <a
                      href={`mailto:${site.contactEmail.trim()}`}
                      className="text-muted-foreground transition-colors hover:text-primary hover:underline"
                    >
                      {site.contactEmail.trim()}
                    </a>
                  ) : (
                    <Link
                      to="/contact"
                      className="text-muted-foreground transition-colors hover:text-primary hover:underline"
                    >
                      {t("nav.contact")}
                    </Link>
                  )}
                </li>
                <li className="flex items-start gap-2">
                  <MapPin className="mt-0.5 h-4 w-4 shrink-0" />
                  <span className="whitespace-pre-line">{site?.address?.trim() || t("landing.location")}</span>
                </li>
              </ul>
              {site?.openingHoursDescription?.trim() ? (
                <div className="mt-4">
                  <h4 className="font-display text-sm font-semibold text-foreground">{t("landing.openingHours")}</h4>
                  <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">{site.openingHoursDescription.trim()}</p>
                </div>
              ) : null}
            </div>
          </div>
          <div className="mt-8 border-t border-border pt-8 text-center text-sm text-muted-foreground">
            {t("landing.copyright", { companyName: legal.companyName })}
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
