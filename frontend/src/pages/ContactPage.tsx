import { useState } from "react";
import { Link } from "react-router-dom";
import { Droplets, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useTranslation } from "react-i18next";
import { PageSeo } from "@/components/seo/PageSeo";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { CompanyBrandName } from "@/components/CompanyBrandName";
import { submitContactForm } from "@/services/contact";
import { toast } from "@/components/ui/sonner";

const initial = { name: "", email: "", subject: "", message: "" };

export default function ContactPage() {
  const { t } = useTranslation();
  const [form, setForm] = useState(initial);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await submitContactForm(form);
      toast.success(t("contact.successMessage"), { position: "bottom-right" });
      setForm(initial);
    } catch {
      toast.error(t("contact.errorMessage"), { position: "bottom-right" });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <PageSeo titleKey="seo.contact.title" descriptionKey="seo.contact.description" />
      <header className="border-b border-border px-4 py-4">
        <div className="container mx-auto flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg gradient-hero">
              <Droplets className="h-5 w-5 text-primary-foreground" />
            </div>
            <CompanyBrandName className="font-display text-xl font-bold text-foreground" />
          </Link>
          <LanguageSwitcher />
        </div>
      </header>

      <main className="container mx-auto max-w-lg flex-1 px-4 py-10 md:py-14">
        <h1 className="font-display text-2xl font-bold tracking-tight text-foreground md:text-3xl">
          {t("contactPage.title")}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground md:text-base">{t("contactPage.subtitle")}</p>
        <p className="mt-1 text-xs text-muted-foreground">{t("contactPage.requiredHint")}</p>

        <form onSubmit={(e) => void handleSubmit(e)} className="mt-8 space-y-5">
          <div className="space-y-2">
            <Label htmlFor="contact-name">{t("contactPage.name")}</Label>
            <Input
              id="contact-name"
              name="name"
              autoComplete="name"
              required
              maxLength={200}
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="bg-card"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="contact-email">{t("contactPage.email")}</Label>
            <Input
              id="contact-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              maxLength={254}
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              className="bg-card"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="contact-subject">{t("contactPage.subject")}</Label>
            <Input
              id="contact-subject"
              name="subject"
              required
              maxLength={200}
              value={form.subject}
              onChange={(e) => setForm((f) => ({ ...f, subject: e.target.value }))}
              className="bg-card"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="contact-message">{t("contactPage.message")}</Label>
            <Textarea
              id="contact-message"
              name="message"
              required
              rows={5}
              maxLength={8000}
              value={form.message}
              onChange={(e) => setForm((f) => ({ ...f, message: e.target.value }))}
              className="min-h-[140px] resize-y bg-card"
            />
          </div>
          <Button type="submit" className="w-full gap-2 gradient-hero border-0" size="lg" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                {t("contactPage.submitting")}
              </>
            ) : (
              t("contactPage.submit")
            )}
          </Button>
        </form>

        <p className="mt-8 text-center text-sm text-muted-foreground">
          <Button variant="link" asChild className="h-auto p-0 text-muted-foreground">
            <Link to="/">{t("placeholder.backHome")}</Link>
          </Button>
        </p>
      </main>
    </div>
  );
}
