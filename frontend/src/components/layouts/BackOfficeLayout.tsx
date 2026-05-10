import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import {
  Droplets,
  LayoutDashboard,
  ShoppingBag,
  Calendar,
  Clock,
  CreditCard,
  Users,
  Settings,
  BarChart3,
  LogOut,
  Globe,
  Key,
  Camera,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import UserAccountMenu from "@/components/UserAccountMenu";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { useAuth } from "@/auth/AuthContext";
import type { AppRole } from "@/auth/roles";
import { hasAnyAppRole, isAdmin } from "@/auth/roles";
import { OrderScanner } from "@/components/backoffice/OrderScanner";
import { CompanyBrandName } from "@/components/CompanyBrandName";

type NavSection = "operations" | "administration";

export type BackOfficeNavItemDef = {
  icon: LucideIcon;
  labelKey: string;
  to: string;
  /** Business roles; matches spec (`ADMIN` / `EMPLOYEE`); API may send `ROLE_*`. */
  roles: readonly AppRole[];
  section: NavSection;
};

type BackOfficeNavItem = BackOfficeNavItemDef & { label: string };

/** Static routing metadata; labels come from i18n via `labelKey` inside the layout. */
export const backOfficeNavItemDefs: BackOfficeNavItemDef[] = [
  {
    icon: LayoutDashboard,
    labelKey: "sidebar.dashboard",
    to: "/backoffice",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "operations",
  },
  {
    icon: ShoppingBag,
    labelKey: "sidebar.orders",
    to: "/backoffice/orders",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "operations",
  },
  {
    icon: Calendar,
    labelKey: "sidebar.planning",
    to: "/backoffice/planning",
    roles: ["ADMIN", "EMPLOYEE"],
    section: "operations",
  },
  {
    icon: Users,
    labelKey: "sidebar.users",
    to: "/backoffice/users",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: ShoppingBag,
    labelKey: "sidebar.servicesPricing",
    to: "/backoffice/services",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: Clock,
    labelKey: "sidebar.timeSlots",
    to: "/backoffice/timeslots",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: BarChart3,
    labelKey: "sidebar.reports",
    to: "/backoffice/reports",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: CreditCard,
    labelKey: "sidebar.payments",
    to: "/backoffice/payments",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: Globe,
    labelKey: "sidebar.multilingualism",
    to: "/backoffice/i18n",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: Key,
    labelKey: "sidebar.api",
    to: "/backoffice/api",
    roles: ["ADMIN"],
    section: "administration",
  },
  {
    icon: Settings,
    labelKey: "sidebar.settings",
    to: "/backoffice/settings",
    roles: ["ADMIN"],
    section: "administration",
  },
];

export function isBackOfficeNavActive(pathname: string, itemTo: string): boolean {
  if (itemTo === "/backoffice") return pathname === "/backoffice";
  return pathname === itemTo || pathname.startsWith(`${itemTo}/`);
}

type OrderScannerTriggerContextValue = {
  openOrderScanner: () => void;
};

const OrderScannerTriggerContext = createContext<OrderScannerTriggerContextValue | null>(null);

export function useOrderScannerTrigger(): OrderScannerTriggerContextValue | null {
  return useContext(OrderScannerTriggerContext);
}

interface BackOfficeLayoutProps {
  children: React.ReactNode;
}

const BackOfficeLayout = ({ children }: BackOfficeLayoutProps) => {
  const { t, i18n } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const userIsAdmin = isAdmin(user?.roles);
  const [orderScannerOpen, setOrderScannerOpen] = useState(false);
  const openOrderScanner = useCallback(() => setOrderScannerOpen(true), []);
  const scannerTriggerValue = useMemo(() => ({ openOrderScanner }), [openOrderScanner]);

  const backOfficeNavItems = useMemo<BackOfficeNavItem[]>(
    () => backOfficeNavItemDefs.map((def) => ({ ...def, label: t(def.labelKey) })),
    [t, i18n.language]
  );

  const visible = backOfficeNavItems.filter((item) => hasAnyAppRole(user?.roles, item.roles));

  const operationsItems = visible.filter((i) => i.section === "operations");
  const adminItems = visible.filter((i) => i.section === "administration");

  const headerLabel =
    visible.find((i) => isBackOfficeNavActive(location.pathname, i.to))?.label ??
    (location.pathname === "/backoffice/profile" ? t("sidebar.profile") : t("sidebar.backOffice"));

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <OrderScannerTriggerContext.Provider value={scannerTriggerValue}>
    <div className="flex min-h-screen bg-background">
      <aside className="hidden w-64 min-w-[12rem] max-w-[20rem] shrink-0 bg-sidebar print:hidden lg:flex lg:flex-col xl:w-72">
        <div className="flex flex-wrap items-center gap-2 border-b border-sidebar-border px-6 py-5">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg gradient-hero">
            <Droplets className="h-4 w-4 text-primary-foreground" />
          </div>
          <CompanyBrandName className="min-w-0 flex-1 font-display text-lg font-bold text-sidebar-foreground" />
          <span className="ml-auto shrink-0 rounded-md bg-sidebar-accent px-2 py-0.5 text-xs font-medium text-sidebar-accent-foreground">
            {userIsAdmin ? t("sidebar.roleAdmin") : t("sidebar.roleStaff")}
          </span>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          {operationsItems.length > 0 && userIsAdmin && (
            <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/50">
              {t("sidebar.operationsSection")}
            </p>
          )}
          {operationsItems.map((item) => {
            const active = isBackOfficeNavActive(location.pathname, item.to);
            return (
              <Link
                key={item.to}
                to={item.to}
                className={`flex min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                  active
                    ? "bg-sidebar-accent text-sidebar-primary"
                    : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                }`}
              >
                <item.icon className="mt-0.5 h-4 w-4 shrink-0" />
                <span className="min-w-0 flex-1 break-words leading-snug">{item.label}</span>
              </Link>
            );
          })}
          {hasAnyAppRole(user?.roles, ["ADMIN", "EMPLOYEE"]) ? (
            <button
              type="button"
              onClick={openOrderScanner}
              className="flex w-full min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-sidebar-foreground/70 transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground"
            >
              <Camera className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
              <span className="min-w-0 flex-1 break-words leading-snug">{t("backoffice.orderScanner.openButton")}</span>
            </button>
          ) : null}
          {adminItems.length > 0 && (
            <>
              <p className="mb-2 mt-6 px-3 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/50">
                {t("sidebar.administrationSection")}
              </p>
              {adminItems.map((item) => {
                const active = isBackOfficeNavActive(location.pathname, item.to);
                return (
                  <Link
                    key={item.to}
                    to={item.to}
                    className={`flex min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                      active
                        ? "bg-sidebar-accent text-sidebar-primary"
                        : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                    }`}
                  >
                    <item.icon className="mt-0.5 h-4 w-4 shrink-0" />
                    <span className="min-w-0 flex-1 break-words leading-snug">{item.label}</span>
                  </Link>
                );
              })}
            </>
          )}
        </nav>
        <div className="border-t border-sidebar-border px-3 py-4">
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
          >
            <LogOut className="mt-0.5 h-4 w-4 shrink-0" />
            <span className="min-w-0 flex-1 break-words leading-snug">{t("sidebar.logout")}</span>
          </button>
        </div>
      </aside>

      <main className="flex-1">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-border bg-card px-6 py-4 print:hidden">
          <h2 className="min-w-0 max-w-full break-words font-display text-lg font-semibold text-foreground">{headerLabel}</h2>
          <div className="flex min-w-0 flex-wrap items-center justify-end gap-2 sm:gap-4">
            {hasAnyAppRole(user?.roles, ["ADMIN", "EMPLOYEE"]) ? (
              <button
                type="button"
                className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground lg:hidden"
                onClick={openOrderScanner}
                aria-label={t("backoffice.orderScanner.openButton")}
                title={t("backoffice.orderScanner.openButton")}
              >
                <Camera className="h-4 w-4" aria-hidden />
              </button>
            ) : null}
            <LanguageSwitcher />
            <UserAccountMenu variant="backoffice" />
          </div>
        </header>
        <div className="p-6 print:p-4">{children}</div>
      </main>
      <OrderScanner open={orderScannerOpen} onOpenChange={setOrderScannerOpen} />
    </div>
    </OrderScannerTriggerContext.Provider>
  );
};

export default BackOfficeLayout;
