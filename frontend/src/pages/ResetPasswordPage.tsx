import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "@/components/ui/sonner";
import { useTranslation } from "react-i18next";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { confirmPasswordReset } from "@/services/auth";
import { ApiError } from "@/lib/api";
import { PASSWORD_POLICY_REGEX } from "@/lib/passwordPolicy";
import LanguageSwitcher from "@/components/LanguageSwitcher";

export default function ResetPasswordPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [token, setToken] = useState(() => (searchParams.get("token") ?? "").trim());

  useEffect(() => {
    setToken((searchParams.get("token") ?? "").trim());
  }, [searchParams]);
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      toast.error(t("resetPassword.error"));
      return;
    }
    if (password !== confirm) {
      toast.error(t("resetPassword.mismatch"));
      return;
    }
    if (!PASSWORD_POLICY_REGEX.test(password)) {
      toast.error(t("resetPassword.passwordPolicy"));
      return;
    }
    setBusy(true);
    try {
      await confirmPasswordReset(token, password);
      toast.success(t("toast.resetPasswordSuccess"));
      window.setTimeout(() => navigate("/login"), 1400);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : t("resetPassword.error"));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 py-12">
      <div className="absolute right-4 top-4">
        <LanguageSwitcher />
      </div>
      <div className="w-full max-w-sm space-y-6">
        <h1 className="font-display text-2xl font-bold text-foreground">{t("resetPassword.title")}</h1>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <Label htmlFor="np">{t("resetPassword.newPassword")}</Label>
            <div className="relative">
              <Input
                id="np"
                className="pr-10"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-sm text-muted-foreground outline-none ring-offset-background hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? t("resetPassword.hidePassword") : t("resetPassword.showPassword")}
              >
                {showPassword ? <EyeOff className="h-4 w-4" aria-hidden /> : <Eye className="h-4 w-4" aria-hidden />}
              </button>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="npc">{t("resetPassword.confirm")}</Label>
            <div className="relative">
              <Input
                id="npc"
                className="pr-10"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
              />
              <button
                type="button"
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-sm text-muted-foreground outline-none ring-offset-background hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? t("resetPassword.hidePassword") : t("resetPassword.showPassword")}
              >
                {showPassword ? <EyeOff className="h-4 w-4" aria-hidden /> : <Eye className="h-4 w-4" aria-hidden />}
              </button>
            </div>
          </div>
          <Button type="submit" className="w-full gradient-hero border-0" disabled={busy}>
            {busy ? "…" : t("resetPassword.submit")}
          </Button>
        </form>
        <p className="text-center text-sm text-muted-foreground">
          <Link to="/login" className="text-primary underline">
            {t("resetPassword.backLogin")}
          </Link>
        </p>
      </div>
    </div>
  );
}
