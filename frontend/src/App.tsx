import { lazy, Suspense } from "react";
import { ThemeProvider } from "next-themes";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthContext";
import ProtectedRoute from "@/auth/ProtectedRoute";
import { PublicSettingsWarmup } from "@/components/PublicSettingsWarmup";
import { DocumentLangSync } from "@/components/seo/DocumentLangSync";

const LandingPage = lazy(() => import("./pages/LandingPage"));
const ServicesPage = lazy(() => import("./pages/ServicesPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const RegisterPage = lazy(() => import("./pages/RegisterPage"));
const ForgotPasswordPage = lazy(() => import("./pages/ForgotPasswordPage"));
const ResetPasswordPage = lazy(() => import("./pages/ResetPasswordPage"));
const ClientDashboard = lazy(() => import("./pages/ClientDashboard"));
const NewOrderPage = lazy(() => import("./pages/NewOrderPage"));
const OrdersPage = lazy(() => import("./pages/OrdersPage"));
const ClientInvoices = lazy(() => import("./pages/ClientInvoices"));
const OrderDetailPage = lazy(() => import("./pages/OrderDetailPage"));
const ProfilePage = lazy(() => import("./pages/ProfilePage"));
const BackOfficeDashboard = lazy(() => import("./pages/BackOfficeDashboard"));
const Planning = lazy(() => import("./pages/backoffice/Planning"));
const Payments = lazy(() => import("./pages/backoffice/Payments"));
const UsersManagement = lazy(() => import("./pages/backoffice/UsersManagement"));
const Reports = lazy(() => import("./pages/backoffice/Reports"));
const MultilingualSettings = lazy(() => import("./pages/backoffice/MultilingualSettings"));
const SettingsPage = lazy(() => import("./pages/backoffice/SettingsPage"));
const ApiPage = lazy(() => import("./pages/backoffice/ApiPage"));
const ServicesTariffs = lazy(() => import("./pages/backoffice/ServicesTariffs"));
const TimeSlotsAdmin = lazy(() => import("./pages/backoffice/TimeSlotsAdmin"));
const OrdersManagement = lazy(() => import("./pages/backoffice/OrdersManagement"));
const StaffOrderDetailPage = lazy(() => import("./pages/backoffice/StaffOrderDetailPage"));
const AdminLegacyRedirect = lazy(() => import("./pages/AdminLegacyRedirect"));
const NotFound = lazy(() => import("./pages/NotFound"));
const ContactPage = lazy(() => import("./pages/ContactPage"));
const LegalNotices = lazy(() => import("./pages/legal/LegalNotices"));
const PrivacyPolicy = lazy(() => import("./pages/legal/PrivacyPolicy"));
const Gdpr = lazy(() => import("./pages/legal/Gdpr"));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
});

function RouteFallback() {
  return (
    <div
      className="flex min-h-[50vh] items-center justify-center bg-background text-sm text-muted-foreground"
      role="status"
      aria-live="polite"
    >
      …
    </div>
  );
}

const App = () => (
  <QueryClientProvider client={queryClient}>
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <AuthProvider>
        <TooltipProvider>
          <Toaster />
          <Sonner />
          <BrowserRouter>
            <DocumentLangSync />
            <PublicSettingsWarmup />
            <Suspense fallback={<RouteFallback />}>
              <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/services" element={<ServicesPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/reset-password" element={<ResetPasswordPage />} />
                <Route path="/contact" element={<ContactPage />} />
                <Route path="/mentions-legales" element={<LegalNotices />} />
                <Route path="/politique-confidentialite" element={<PrivacyPolicy />} />
                <Route path="/rgpd" element={<Gdpr />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />

                <Route
                  path="/dashboard"
                  element={
                    <ProtectedRoute>
                      <ClientDashboard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/new-order"
                  element={
                    <ProtectedRoute>
                      <NewOrderPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/orders"
                  element={
                    <ProtectedRoute>
                      <OrdersPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/orders/:id"
                  element={
                    <ProtectedRoute>
                      <OrderDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/invoices"
                  element={
                    <ProtectedRoute>
                      <ClientInvoices />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard/profile"
                  element={
                    <ProtectedRoute>
                      <ProfilePage />
                    </ProtectedRoute>
                  }
                />

                <Route
                  path="/backoffice"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN", "EMPLOYEE"]} forbiddenRedirect="/dashboard">
                      <BackOfficeDashboard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/orders"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN", "EMPLOYEE"]} forbiddenRedirect="/dashboard">
                      <OrdersManagement />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/orders/:id"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN", "EMPLOYEE"]} forbiddenRedirect="/dashboard">
                      <StaffOrderDetailPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/planning"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN", "EMPLOYEE"]} forbiddenRedirect="/dashboard">
                      <Planning />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/profile"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN", "EMPLOYEE"]} forbiddenRedirect="/dashboard">
                      <ProfilePage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/users"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <UsersManagement />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/services"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <ServicesTariffs />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/timeslots"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <TimeSlotsAdmin />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/reports"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <Reports />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/payments"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <Payments />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/i18n"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <MultilingualSettings />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/api"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <ApiPage />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/backoffice/settings"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN"]} forbiddenRedirect="/backoffice">
                      <SettingsPage />
                    </ProtectedRoute>
                  }
                />

                <Route
                  path="/admin/*"
                  element={
                    <ProtectedRoute allowRoles={["ADMIN", "EMPLOYEE"]} forbiddenRedirect="/dashboard">
                      <AdminLegacyRedirect />
                    </ProtectedRoute>
                  }
                />

                <Route path="*" element={<NotFound />} />
              </Routes>
            </Suspense>
          </BrowserRouter>
        </TooltipProvider>
      </AuthProvider>
    </ThemeProvider>
  </QueryClientProvider>
);

export default App;
