import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { toast } from "@/components/ui/sonner";
import { Droplets } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useTranslation } from "react-i18next";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { CompanyBrandName } from "@/components/CompanyBrandName";
import { requestPasswordReset } from "@/services/auth";
import { ApiError } from "@/lib/api";

export default function ForgotPasswordPage() {
  const { t } = useTranslation();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      await requestPasswordReset(email);
      toast.success(t("toast.forgotPasswordSent"), { duration: 6000 });
      setEmail("");
    } catch (err) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : t("forgotPassword.errorGeneric");
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center justify-between">
            <Link to="/" className="flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg gradient-hero">
                <Droplets className="h-5 w-5 text-primary-foreground" />
              </div>
              <CompanyBrandName className="font-display text-xl font-bold text-foreground" />
            </Link>
            <LanguageSwitcher />
          </div>

          <h1 className="font-display text-2xl font-bold text-foreground">{t("forgotPassword.title")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">{t("forgotPassword.subtitle")}</p>

          <form className="mt-8 space-y-4" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <Label htmlFor="forgot-email">{t("auth.email")}</Label>
              <Input
                id="forgot-email"
                type="email"
                autoComplete="email"
                placeholder={t("auth.emailPlaceholder")}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={isLoading}
              />
            </div>
            <Button
              type="submit"
              disabled={isLoading}
              className="w-full gradient-hero border-0 text-primary-foreground hover:opacity-90"
              size="lg"
            >
              {isLoading ? "…" : t("forgotPassword.submit")}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            <Link
              to={{ pathname: "/login", search: location.search }}
              className="font-medium text-primary hover:underline"
            >
              {t("forgotPassword.backToLogin")}
            </Link>
          </p>
        </div>
      </div>

      <div className="hidden w-1/2 gradient-hero lg:flex lg:items-center lg:justify-center">
        <div className="max-w-md px-8 text-center">
          <Droplets className="mx-auto mb-6 h-16 w-16 text-primary-foreground/80 animate-float" />
          <h2 className="font-display text-3xl font-bold text-primary-foreground">{t("auth.welcomeTitle")}</h2>
          <p className="mt-4 text-primary-foreground/80">{t("auth.welcomeSub")}</p>
        </div>
      </div>
    </div>
  );
}
