import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { useTranslation } from "react-i18next";
import { applyPreferredLanguage } from "@/i18n/applyPreferredLanguage";
import { clearAuthStorage, getStoredToken, persistAuthSession } from "@/lib/authStorage";
import { ApiError } from "@/lib/api";
import { login as loginRequest } from "@/services/auth";
import { getMe } from "@/services/users";
import type { User } from "@/types";

type AuthContextValue = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => void;
  refreshUser: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    const token = getStoredToken();
    if (!token) {
      setUser(null);
      return;
    }
    const me = await getMe();
    await applyPreferredLanguage(i18n, me.preferredLanguage);
    setUser(me);
  }, [i18n]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!getStoredToken()) {
        setLoading(false);
        return;
      }
      try {
        const me = await getMe();
        if (!cancelled) {
          await applyPreferredLanguage(i18n, me.preferredLanguage);
          setUser(me);
        }
      } catch {
        clearAuthStorage();
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [i18n]);

  const login = useCallback(async (email: string, password: string): Promise<User> => {
    let res: Awaited<ReturnType<typeof loginRequest>>;
    try {
      res = await loginRequest(email, password);
    } catch (e) {
      if (e instanceof ApiError) {
        const b = e.body;
        if (
          b != null &&
          typeof b === "object" &&
          (b as { error?: string }).error === "ACCOUNT_SUSPENDED"
        ) {
          throw e;
        }
        throw new ApiError(`Connexion : ${e.message}`, e.status, e.body);
      }
      throw e;
    }

    persistAuthSession(res.token, res.email, res.roles);

    await applyPreferredLanguage(i18n, res.preferredLanguage);

    try {
      const me = await getMe();
      await applyPreferredLanguage(i18n, me.preferredLanguage);
      setUser(me);
      return me;
    } catch (e) {
      // Token was stored but /users/me failed — roll back to avoid a "logged in" UI with no user
      clearAuthStorage();
      if (e instanceof ApiError) {
        throw new ApiError(
          `Profil (${e.status}) : ${e.message}. Le jeton a été refusé ou GET /api/users/me a échoué.`,
          e.status,
          e.body
        );
      }
      throw e;
    }
  }, [i18n]);

  const logout = useCallback(() => {
    clearAuthStorage();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, logout, refreshUser }),
    [user, loading, login, logout, refreshUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
