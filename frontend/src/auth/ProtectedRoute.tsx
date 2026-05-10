import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { AppRole } from "./roles";
import { hasAnyAppRole } from "./roles";

type ProtectedRouteProps = {
  children: React.ReactNode;
  /** If set, the user must have at least one of these roles (API may send `ROLE_ADMIN` or `ADMIN`). */
  allowRoles?: readonly AppRole[];
  /** When authenticated but missing `allowRoles`, redirect here (default: `/dashboard`). */
  forbiddenRedirect?: string;
};

export default function ProtectedRoute({
  children,
  allowRoles,
  forbiddenRedirect = "/dashboard",
}: ProtectedRouteProps) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted-foreground">
        Loading…
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (allowRoles?.length && !hasAnyAppRole(user.roles, allowRoles)) {
    return <Navigate to={forbiddenRedirect} replace />;
  }

  return <>{children}</>;
}
