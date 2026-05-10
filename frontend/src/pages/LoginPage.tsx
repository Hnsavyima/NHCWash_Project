import { useState, useEffect } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { Droplets, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useTranslation } from "react-i18next";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { CompanyBrandName } from "@/components/CompanyBrandName";
import { toast } from "@/components/ui/sonner";
import { useAuth } from "@/auth/AuthContext";
import { buildPostLoginDestination } from "@/auth/postLoginRedirect";
import { ApiError } from "@/lib/api";

const LoginPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { login, user } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [suspensionData, setSuspensionData] = useState<{ isSuspended: true; reason: string } | null>(null);

  useEffect(() => {
    setSuspensionData(null);
  }, [email]);

  useEffect(() => {
    if (!user) return;
    const dest = buildPostLoginDestination(user.roles, searchParams.get("redirect"), searchParams.get("service"));
    navigate(dest, { replace: true });
  }, [user, navigate, searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const me = await login(email, password);
      const dest = buildPostLoginDestination(me.roles, searchParams.get("redirect"), searchParams.get("service"));
      navigate(dest, { replace: true });
    } catch (err: unknown) {
      const e = err as { response?: { data?: { reason?: string; error?: string } }; data?: { reason?: string; error?: string } };
      console.log("Error data received:", err instanceof ApiError ? err.body : e.response?.data);
      const reason = e.response?.data?.reason || e.data?.reason;
      const payload = (
        err instanceof ApiError ? err.body : (e.response?.data ?? e.data)
      ) as Record<string, unknown> | undefined;
      const isSuspended =
        typeof payload === "object" &&
        payload !== null &&
        (payload as { error?: string }).error === "ACCOUNT_SUSPENDED";
      if (isSuspended) {
        const fromPayload =
          typeof payload.reason === "string" ? String(payload.reason).trim() : "";
        const fromAxios = typeof reason === "string" ? reason.trim() : "";
        setSuspensionData({
          isSuspended: true,
          reason: fromAxios || fromPayload,
        });
        return;
      }
      setSuspensionData(null);
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : t("auth.errorGeneric");
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const getTranslatedReason = (rawReason: string | undefined) => {
    if (!rawReason) return "";

    const unpaidBase = t("suspension.quickReplies.unpaidText", {
      lng: "fr",
      defaultValue:
        "Votre compte a été suspendu suite à un incident de paiement ou des factures en attente de règlement.",
    });
    const fraudBase = t("suspension.quickReplies.fraudText", {
      lng: "fr",
      defaultValue:
        "Nous avons détecté une activité suspecte ou frauduleuse sur votre compte. Par mesure de sécurité, celui-ci a été temporairement désactivé.",
    });
    const tosBase = t("suspension.quickReplies.tosText", {
      lng: "fr",
      defaultValue:
        "Votre compte a été suspendu suite au non-respect de nos conditions générales d'utilisation.",
    });

    const trimmed = rawReason.trim();
    if (trimmed === unpaidBase.trim()) return t("suspension.quickReplies.unpaidText");
    if (trimmed === fraudBase.trim()) return t("suspension.quickReplies.fraudText");
    if (trimmed === tosBase.trim()) return t("suspension.quickReplies.tosText");

    return rawReason;
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
          <h1 className="font-display text-2xl font-bold text-foreground">{t("auth.loginTitle")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">{t("auth.loginSub")}</p>

          {suspensionData?.isSuspended ? (
            <>
              {/* Premium Suspension Alert Card */}
              <div className="relative mb-6 mt-4 overflow-hidden rounded-xl border border-gray-100 bg-white p-6 shadow-md animate-in fade-in slide-in-from-top-4 duration-500 dark:border-gray-700 dark:bg-gray-800">
              {/* Top Accent Line */}
              <div className="absolute left-0 top-0 h-1 w-full bg-gradient-to-r from-red-500 to-rose-500" />

              <div className="flex items-start gap-4">
                {/* Circular Icon Badge */}
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden
                  >
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                </div>

                <div className="flex-1">
                  <h3 className="text-base font-bold text-gray-900 dark:text-white">{t("suspension.title")}</h3>
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{t("suspension.description")}</p>

                  {/* Styled Reason Badge */}
                  <div className="mt-3 inline-flex items-center rounded-md border border-red-100 bg-red-50 px-3 py-1.5 dark:border-red-800/30 dark:bg-red-900/20">
                    <span className="text-sm font-semibold text-red-700 dark:text-red-300">
                      {suspensionData.reason ? (
                        <>
                          {t("suspension.reasonLabel")}{" "}
                          <span className="whitespace-pre-wrap font-semibold">
                            {getTranslatedReason(suspensionData.reason)}
                          </span>
                        </>
                      ) : (
                        t("suspension.noReasonFallback")
                      )}
                    </span>
                  </div>
                </div>
              </div>

              {/* Full-width Action Button */}
              <div className="mt-5">
                <Link
                  to="/contact"
                  className="flex w-full items-center justify-center gap-2 rounded-lg bg-red-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:bg-red-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-600"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden
                  >
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
                  </svg>
                  {t("suspension.contactSupport")}
                </Link>
              </div>
            </div>
            </>
          ) : null}

          <form className="mt-8 space-y-4" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <Label htmlFor="email">{t("auth.email")}</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder={t("auth.emailPlaceholder")}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">{t("auth.password")}</Label>
                <Link to="/forgot-password" className="text-xs text-primary hover:underline">
                  {t("auth.forgotPassword")}
                </Link>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <Button
              type="submit"
              disabled={submitting}
              className="w-full gradient-hero border-0 text-primary-foreground hover:opacity-90"
              size="lg"
            >
              {submitting ? "…" : t("auth.loginBtn")}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            {t("auth.noAccount")}{" "}
            <Link to={{ pathname: "/register", search: location.search }} className="font-medium text-primary hover:underline">
              {t("auth.createAccount")}
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
};

export default LoginPage;
