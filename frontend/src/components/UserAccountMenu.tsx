import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LogOut, UserRound } from "lucide-react";
import { useTranslation } from "react-i18next";
import { resolveAvatarUrl } from "@/lib/avatarUrl";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/auth/AuthContext";
import { getProfilePath } from "@/auth/profilePath";

type Variant = "client" | "backoffice";

type Props = {
  variant?: Variant;
};

export default function UserAccountMenu({ variant = "client" }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const profileHref = getProfilePath(user?.roles);

  const initials =
    user?.firstName || user?.lastName
      ? `${user.firstName?.[0] ?? ""}${user.lastName?.[0] ?? ""}`.toUpperCase() || "?"
      : user?.email?.[0]?.toUpperCase() ?? "?";

  const [avatarFailed, setAvatarFailed] = useState(false);
  const avatarSrc = user ? resolveAvatarUrl(user.avatarUrl) : undefined;

  useEffect(() => {
    setAvatarFailed(false);
  }, [user?.avatarUrl]);

  const displayName =
    user?.firstName || user?.lastName
      ? [user.firstName, user.lastName].filter(Boolean).join(" ")
      : user?.email ?? "";

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const triggerClasses =
    variant === "backoffice"
      ? "h-9 w-9 shrink-0 gap-0 rounded-full border-0 bg-primary/10 p-0 text-xs font-semibold text-primary hover:bg-primary/15 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
      : "h-9 w-9 shrink-0 gap-0 rounded-full border border-border bg-card p-0 text-xs font-semibold text-primary hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button type="button" variant="ghost" className={triggerClasses} aria-label={t("nav.accountMenu")}>
          {avatarSrc && !avatarFailed ? (
            <img
              src={avatarSrc}
              alt=""
              className="h-full w-full rounded-full object-cover"
              onError={() => setAvatarFailed(true)}
            />
          ) : (
            <span className="pointer-events-none select-none">{initials}</span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-[200px]" onCloseAutoFocus={(e) => e.preventDefault()}>
        <DropdownMenuLabel className="font-normal">
          <p className="truncate text-sm font-medium text-foreground">{displayName}</p>
          <p className="truncate text-xs font-normal text-muted-foreground">{user?.email}</p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to={profileHref} className="flex cursor-pointer items-center gap-2">
            <UserRound className="h-4 w-4" />
            {t("nav.myProfile")}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          className="cursor-pointer text-destructive focus:text-destructive"
          onSelect={(e) => {
            e.preventDefault();
            handleLogout();
          }}
        >
          <LogOut className="mr-2 h-4 w-4" />
          {t("nav.logout")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
