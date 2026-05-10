import { Link, useLocation, useNavigate } from "react-router-dom";
import { Droplets, LayoutDashboard, ShoppingBag, PlusCircle, FileText, User, LogOut } from "lucide-react";
import { useTranslation } from "react-i18next";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { CompanyBrandName } from "@/components/CompanyBrandName";
import UserAccountMenu from "@/components/UserAccountMenu";
import { useAuth } from "@/auth/AuthContext";

const ClientLayout = ({ children }: { children: React.ReactNode }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const navItems = [
    { icon: LayoutDashboard, label: t("sidebar.dashboard"), to: "/dashboard" },
    { icon: PlusCircle, label: t("sidebar.newOrder"), to: "/dashboard/new-order" },
    { icon: ShoppingBag, label: t("sidebar.myOrders"), to: "/dashboard/orders" },
    { icon: FileText, label: t("sidebar.invoices"), to: "/dashboard/invoices" },
    { icon: User, label: t("sidebar.profile"), to: "/dashboard/profile" },
  ];

  return (
    <div className="flex min-h-screen bg-background">
      <aside className="hidden w-64 min-w-[12rem] max-w-[20rem] shrink-0 border-r border-border bg-card lg:block xl:w-72">
        <div className="flex h-full flex-col">
          <div className="flex items-center gap-2 border-b border-border px-6 py-5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg gradient-hero">
              <Droplets className="h-4 w-4 text-primary-foreground" />
            </div>
            <CompanyBrandName className="font-display text-lg font-bold text-foreground" />
          </div>
          <nav className="flex-1 space-y-1 px-3 py-4">
            {navItems.map((item) => {
              const active = location.pathname === item.to;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`flex min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                    active ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted hover:text-foreground"
                  }`}
                >
                  <item.icon className="mt-0.5 h-4 w-4 shrink-0" />
                  <span className="min-w-0 flex-1 break-words leading-snug">{item.label}</span>
                </Link>
              );
            })}
          </nav>
          <div className="border-t border-border px-3 py-4">
            <button
              type="button"
              onClick={handleLogout}
              className="flex w-full min-w-0 items-start gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              <LogOut className="mt-0.5 h-4 w-4 shrink-0" />
              <span className="min-w-0 flex-1 break-words leading-snug">{t("nav.logout")}</span>
            </button>
          </div>
        </div>
      </aside>

      <main className="flex-1">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-border bg-card px-6 py-4">
          <div className="flex items-center gap-2 lg:hidden">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg gradient-hero">
              <Droplets className="h-4 w-4 text-primary-foreground" />
            </div>
            <CompanyBrandName className="font-display font-bold text-foreground" />
          </div>
          <div className="hidden lg:block">
            <h2 className="font-display text-lg font-semibold text-foreground">
              {[...navItems]
                .sort((a, b) => b.to.length - a.to.length)
                .find((i) => location.pathname === i.to || location.pathname.startsWith(`${i.to}/`))?.label ||
                t("sidebar.dashboard")}
            </h2>
          </div>
          <div className="flex min-w-0 flex-wrap items-center justify-end gap-2 sm:gap-4">
            <LanguageSwitcher />
            <UserAccountMenu variant="client" />
          </div>
        </header>
        <div className="p-6">{children}</div>
      </main>
    </div>
  );
};

export default ClientLayout;
