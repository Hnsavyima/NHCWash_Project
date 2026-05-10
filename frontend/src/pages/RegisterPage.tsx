import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "@/components/ui/sonner";
import { Camera, Droplets, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { useTranslation } from "react-i18next";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { CompanyBrandName } from "@/components/CompanyBrandName";
import { useAuth } from "@/auth/AuthContext";
import { applyPreferredLanguage } from "@/i18n/applyPreferredLanguage";
import {
  PROFILE_LANG_OPTIONS,
  parsePreferredToUi,
  profileLangToBackend,
  type UiLangCode,
} from "@/lib/profilePreferredLanguage";
import { registerWithAvatar } from "@/services/auth";
import { ApiError } from "@/lib/api";
import { PASSWORD_POLICY_REGEX } from "@/lib/passwordPolicy";

const PHONE_REGEX = /^\+?[0-9\s\-().]{8,22}$/;

type RegisterFormValues = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  preferredLangUi: UiLangCode;
  password: string;
  confirmPassword: string;
};

function buildRegisterSchema(t: (key: string) => string) {
  return z
    .object({
      firstName: z.string().trim().min(1, { message: t("auth.required") }),
      lastName: z.string().trim().min(1, { message: t("auth.required") }),
      email: z
        .string()
        .trim()
        .min(1, { message: t("auth.required") })
        .email({ message: t("auth.invalidEmail") }),
      phone: z
        .string()
        .trim()
        .min(1, { message: t("auth.phoneRequired") })
        .regex(PHONE_REGEX, { message: t("auth.phoneInvalid") }),
      preferredLangUi: z.enum(["fr", "en", "nl", "de"]),
      password: z.string().min(1, { message: t("auth.required") }),
      confirmPassword: z.string().min(1, { message: t("auth.required") }),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: t("auth.passwordMismatch"),
      path: ["confirmPassword"],
    })
    .refine((data) => PASSWORD_POLICY_REGEX.test(data.password), {
      message: t("auth.passwordPolicy"),
      path: ["password"],
    });
}

const RegisterPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [registered, setRegistered] = useState(false);

  const schema = useMemo(() => buildRegisterSchema(t), [t, i18n.language]);

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      preferredLangUi: parsePreferredToUi(i18n.language),
      password: "",
      confirmPassword: "",
    },
  });

  useEffect(() => {
    if (!avatarFile) {
      setAvatarPreview(null);
      return;
    }
    const url = URL.createObjectURL(avatarFile);
    setAvatarPreview(url);
    return () => {
      URL.revokeObjectURL(url);
    };
  }, [avatarFile]);

  useEffect(() => {
    if (user) navigate("/dashboard", { replace: true });
  }, [user, navigate]);

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      const preferredLanguage = profileLangToBackend(values.preferredLangUi);
      await registerWithAvatar(
        {
          email: values.email,
          password: values.password,
          firstName: values.firstName,
          lastName: values.lastName,
          phone: values.phone,
          preferredLanguage,
        },
        avatarFile
      );
      setRegistered(true);
      toast.success(t("toast.registerSuccess"));
      window.setTimeout(() => {
        navigate({ pathname: "/login", search: location.search });
      }, 1600);
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : t("auth.errorGeneric");
      toast.error(message);
    }
  });

  const submitting = form.formState.isSubmitting;

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
          <h1 className="font-display text-2xl font-bold text-foreground">{t("auth.registerTitle")}</h1>
          <p className="mt-2 text-sm text-muted-foreground">{t("auth.registerSub")}</p>

          <Form {...form}>
            <form className="mt-8 space-y-4" onSubmit={onSubmit}>
              <div className="flex flex-col items-center gap-2">
                <p className="text-center text-xs font-medium text-muted-foreground">{t("auth.avatarSectionOptional")}</p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  className="sr-only"
                  aria-required={false}
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    setAvatarFile(f && f.size > 0 ? f : null);
                  }}
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="relative flex h-24 w-24 cursor-pointer items-center justify-center overflow-hidden rounded-full border-2 border-dashed border-border bg-muted/50 text-muted-foreground transition-colors hover:border-primary/50 hover:bg-muted"
                >
                  {avatarPreview ? (
                    <img src={avatarPreview} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <Camera className="h-9 w-9" aria-hidden />
                  )}
                </button>
                <p className="text-center text-xs text-muted-foreground">{t("auth.avatarHint")}</p>
                {avatarFile ? (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-auto py-1 text-xs text-muted-foreground hover:text-foreground"
                    onClick={() => {
                      setAvatarFile(null);
                      if (fileInputRef.current) fileInputRef.current.value = "";
                    }}
                  >
                    {t("auth.clearSelectedPhoto")}
                  </Button>
                ) : null}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <FormField
                  control={form.control}
                  name="firstName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t("auth.firstName")}</FormLabel>
                      <FormControl>
                        <Input placeholder={t("auth.firstNamePlaceholder")} autoComplete="given-name" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t("auth.lastName")}</FormLabel>
                      <FormControl>
                        <Input placeholder={t("auth.lastNamePlaceholder")} autoComplete="family-name" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("auth.email")}</FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        autoComplete="email"
                        placeholder={t("auth.emailPlaceholder")}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("auth.phone")}</FormLabel>
                    <FormControl>
                      <Input type="tel" autoComplete="tel" placeholder={t("auth.phonePlaceholder")} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("auth.password")}</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <Input
                          className="pr-10"
                          type={showPassword ? "text" : "password"}
                          autoComplete="new-password"
                          placeholder="••••••••"
                          {...field}
                        />
                        <button
                          type="button"
                          className="absolute right-3 top-1/2 -translate-y-1/2 rounded-sm text-muted-foreground outline-none ring-offset-background hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                          onClick={() => setShowPassword(!showPassword)}
                          aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}
                        >
                          {showPassword ? <EyeOff className="h-4 w-4" aria-hidden /> : <Eye className="h-4 w-4" aria-hidden />}
                        </button>
                      </div>
                    </FormControl>
                    <p className="text-xs text-muted-foreground">{t("auth.passwordPolicy")}</p>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="confirmPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("auth.confirmPassword")}</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <Input
                          className="pr-10"
                          type={showConfirmPassword ? "text" : "password"}
                          autoComplete="new-password"
                          placeholder="••••••••"
                          {...field}
                        />
                        <button
                          type="button"
                          className="absolute right-3 top-1/2 -translate-y-1/2 rounded-sm text-muted-foreground outline-none ring-offset-background hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                          onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                          aria-label={
                            showConfirmPassword ? t("auth.hidePassword") : t("auth.showPassword")
                          }
                        >
                          {showConfirmPassword ? (
                            <EyeOff className="h-4 w-4" aria-hidden />
                          ) : (
                            <Eye className="h-4 w-4" aria-hidden />
                          )}
                        </button>
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="preferredLangUi"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("auth.preferredLanguage")}</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={(v) => {
                        const code = v as UiLangCode;
                        field.onChange(code);
                        void applyPreferredLanguage(i18n, profileLangToBackend(code));
                      }}
                    >
                      <FormControl>
                        <SelectTrigger id="register-preferred-lang" className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {PROFILE_LANG_OPTIONS.map((opt) => (
                          <SelectItem key={opt.code} value={opt.code}>
                            {t(opt.labelKey)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Button
                type="submit"
                disabled={submitting || registered}
                className="w-full gradient-hero border-0 text-primary-foreground hover:opacity-90"
                size="lg"
              >
                {submitting ? "…" : t("auth.registerBtn")}
              </Button>
            </form>
          </Form>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            {t("auth.hasAccount")}{" "}
            <Link to={{ pathname: "/login", search: location.search }} className="font-medium text-primary hover:underline">
              {t("auth.loginLink")}
            </Link>
          </p>

          <p className="mt-4 text-center text-xs text-muted-foreground">
            {t("auth.terms")}{" "}
            <a href="#" className="underline">
              {t("auth.termsLink")}
            </a>{" "}
            {t("auth.and")}{" "}
            <a href="#" className="underline">
              {t("auth.privacyLink")}
            </a>
            .
          </p>
        </div>
      </div>

      <div className="hidden w-1/2 gradient-hero lg:flex lg:items-center lg:justify-center">
        <div className="max-w-md px-8 text-center">
          <Droplets className="mx-auto mb-6 h-16 w-16 text-primary-foreground/80 animate-float" />
          <h2 className="font-display text-3xl font-bold text-primary-foreground">{t("auth.joinTitle")}</h2>
          <p className="mt-4 text-primary-foreground/80">{t("auth.joinSub")}</p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
