import { useState, useCallback, useEffect } from "react";
import { useParams, Link, useSearchParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle,
  Download,
  ExternalLink,
  Loader2,
  Package,
  Printer,
  QrCode,
  XCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useTranslation } from "react-i18next";
import ClientLayout from "@/components/layouts/ClientLayout";
import { OrderTrackingStepper } from "@/components/orders/OrderTrackingStepper";
import {
  orderComputedSubtotal,
  orderDisplayTotal,
  orderLineSubtotal,
  orderPublicReference,
  statusBadgeClass,
} from "@/types";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { downloadOrderInvoicePdf, getOrderById } from "@/services/orders";
import { confirmStripePayment, createCheckoutSession } from "@/services/payments";
import { tCatalog } from "@/i18n/catalog";
import { toast } from "@/components/ui/sonner";
import { ApiError } from "@/lib/api";
import { dateLocaleFromI18n } from "@/lib/dateLocaleFromI18n";
import { isClientInvoiceDownloadAllowed } from "@/utils/orderStatus";

type PaymentBannerState = "success" | "cancel" | "verifying" | null;

const OrderDetailPage = () => {
  const { t, i18n } = useTranslation();
  const { id } = useParams();
  const orderId = Number(id);
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const [isDownloading, setIsDownloading] = useState(false);
  const [paymentState, setPaymentState] = useState<PaymentBannerState>(null);
  const [retryCheckoutLoading, setRetryCheckoutLoading] = useState(false);

  const { data: order, isLoading, error } = useQuery({
    queryKey: ["order", orderId],
    queryFn: () => getOrderById(orderId),
    enabled: Number.isFinite(orderId),
    refetchOnWindowFocus: true,
  });

  useEffect(() => {
    const payment = searchParams.get("payment");
    const sessionId = searchParams.get("session_id");

    if (payment === "cancel") {
      setPaymentState("cancel");
      setSearchParams({}, { replace: true });
      return;
    }

    if (payment !== "success" || !sessionId || !Number.isFinite(orderId)) {
      return;
    }

    let cancelled = false;
    setPaymentState("verifying");
    (async () => {
      try {
        await confirmStripePayment({ orderId, session_id: sessionId });
        if (cancelled) return;
        await queryClient.invalidateQueries({ queryKey: ["order", orderId] });
        await queryClient.invalidateQueries({ queryKey: ["orders"] });
        await queryClient.refetchQueries({ queryKey: ["order", orderId] });
        if (!cancelled) {
          setPaymentState("success");
          setSearchParams({}, { replace: true });
        }
      } catch {
        if (!cancelled) {
          setPaymentState(null);
          toast.error(t("toast.paymentVerifyError"));
          setSearchParams({}, { replace: true });
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [searchParams, orderId, setSearchParams, queryClient, t]);

  const canInvoicePdf = Boolean(
    order && (isClientInvoiceDownloadAllowed(order) || paymentState === "success")
  );

  const handleInvoiceDownload = useCallback(async () => {
    if (!order || !canInvoicePdf || isDownloading) return;
    setIsDownloading(true);
    try {
      const blob = await downloadOrderInvoicePdf(order.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `facture_CMD-${String(order.id).padStart(3, "0")}.pdf`;
      a.style.display = "none";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      toast.success(t("toast.invoiceDownloaded"));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : t("toast.invoiceDownloadError"));
    } finally {
      setIsDownloading(false);
    }
  }, [order, canInvoicePdf, isDownloading, t]);

  const handlePrintPaymentQr = useCallback(
    (imageSrc: string, reference: string) => {
      const w = window.open("", "_blank");
      if (!w) {
        toast.error(t("orderDetail.paymentQrPopupBlocked"));
        return;
      }
      w.document.open();
      w.document.write(
        "<!DOCTYPE html><html><head><meta charset='utf-8'/><title>QR</title></head><body style='margin:0;display:flex;min-height:100vh;align-items:center;justify-content:center;background:#fff'></body></html>"
      );
      w.document.close();
      const img = w.document.createElement("img");
      img.src = imageSrc;
      img.alt = reference;
      img.style.maxWidth = "100%";
      img.style.height = "auto";
      w.document.body.appendChild(img);
      img.onload = () => {
        w.focus();
        w.print();
        w.close();
      };
    },
    [t]
  );

  const handleRetryCheckout = useCallback(async () => {
    if (!order || !Number.isFinite(orderId)) return;
    if (order.status === "CANCELLED" || isClientInvoiceDownloadAllowed(order)) {
      toast.info(t("orderDetail.invoicePaidOnly"));
      return;
    }
    setRetryCheckoutLoading(true);
    try {
      const { url } = await createCheckoutSession(order.id);
      window.location.href = url;
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : t("auth.errorGeneric"));
      setRetryCheckoutLoading(false);
    }
  }, [order, orderId, t]);

  if (!Number.isFinite(orderId)) {
    return (
      <ClientLayout>
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <Package className="h-16 w-16 text-muted-foreground/50" />
          <h2 className="mt-4 font-display text-xl font-bold text-foreground">{t("orderDetail.notFound")}</h2>
          <Link to="/dashboard/orders" className="mt-4 text-sm text-primary hover:underline">
            {t("orderDetail.backToOrders")}
          </Link>
        </div>
      </ClientLayout>
    );
  }

  if (isLoading) {
    return (
      <ClientLayout>
        <div className="py-12 text-center text-muted-foreground">…</div>
      </ClientLayout>
    );
  }

  if (error || !order) {
    return (
      <ClientLayout>
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <Package className="h-16 w-16 text-muted-foreground/50" />
          <h2 className="mt-4 font-display text-xl font-bold text-foreground">{t("orderDetail.notFound")}</h2>
          <p className="mt-2 text-sm text-destructive">{(error as Error)?.message}</p>
          <Link to="/dashboard/orders" className="mt-4 text-sm text-primary hover:underline">
            {t("orderDetail.backToOrders")}
          </Link>
        </div>
      </ClientLayout>
    );
  }

  const badgeClass = statusBadgeClass(order.status);

  const formatDate = (iso: string | null) =>
    iso
      ? new Date(iso).toLocaleDateString(dateLocaleFromI18n(i18n.language), {
          day: "numeric",
          month: "long",
          year: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        })
      : "—";

  const subtotal = orderComputedSubtotal(order);
  const total = orderDisplayTotal(order);

  const orderRef = orderPublicReference(order.id);
  const showOnSitePaymentQr =
    order.paymentStatus?.toUpperCase() === "PENDING" && order.checkoutPaymentMode === "CASH_ON_SITE";
  const paymentQrSrc = showOnSitePaymentQr
    ? `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(orderRef)}`
    : "";

  return (
    <ClientLayout>
      <div className="mx-auto w-full max-w-3xl space-y-6">
        {paymentState === "verifying" && (
          <div
            className="relative w-full overflow-hidden rounded-xl border border-primary/20 bg-gradient-to-br from-primary/5 via-background to-muted/40 p-8 shadow-sm"
            role="status"
            aria-live="polite"
          >
            <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-primary/10 via-transparent to-transparent" />
            <div className="relative flex flex-col items-center text-center">
              <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-full border border-primary/20 bg-background/80 shadow-inner">
                <Loader2 className="h-9 w-9 animate-spin text-primary" aria-hidden />
              </div>
              <p className="font-display text-lg font-semibold tracking-tight text-foreground">
                {t("orderDetail.paymentVerifyingTitle")}
              </p>
              <p className="mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">
                {t("orderDetail.paymentVerifyingHint")}
              </p>
              <div className="mt-6 flex gap-1.5">
                {[0, 1, 2].map((i) => (
                  <span
                    key={i}
                    className="h-1.5 w-8 animate-pulse rounded-full bg-primary/30"
                    style={{ animationDelay: `${i * 160}ms` }}
                  />
                ))}
              </div>
            </div>
          </div>
        )}

        {paymentState === "success" && (
          <div className="w-full rounded-lg border border-green-200 bg-green-50 p-6 text-center shadow-sm dark:border-green-900/50 dark:bg-green-950/30">
            <div className="flex flex-col items-center">
              <CheckCircle className="mb-4 h-16 w-16 text-green-500 dark:text-green-400" aria-hidden />
              <h2 className="font-display text-2xl font-bold text-green-900 dark:text-green-100">
                {t("orderDetail.paymentSuccessTitle")}
              </h2>
              <p className="mt-2 max-w-lg text-green-700 dark:text-green-200/90">{t("orderDetail.paymentSuccessSubtitle")}</p>
              <Button
                type="button"
                size="lg"
                className="mt-6 gap-2 bg-green-600 text-white hover:bg-green-700 dark:bg-green-600 dark:hover:bg-green-500"
                disabled={!canInvoicePdf || isDownloading}
                title={!canInvoicePdf ? t("orderDetail.invoicePaidOnly") : undefined}
                onClick={handleInvoiceDownload}
              >
                {isDownloading ? <Loader2 className="h-5 w-5 animate-spin" /> : <Download className="h-5 w-5" />}
                {t("orderDetail.paymentSuccessDownload")}
              </Button>
            </div>
          </div>
        )}

        {paymentState === "cancel" && (
          <div className="flex w-full flex-col gap-4 rounded-lg border border-orange-200 bg-orange-50 p-4 shadow-sm sm:flex-row sm:items-center dark:border-orange-900/50 dark:bg-orange-950/25">
            <AlertCircle className="h-10 w-10 shrink-0 text-orange-500 dark:text-orange-400" aria-hidden />
            <p className="flex-1 text-sm leading-relaxed text-orange-800 dark:text-orange-100/90">
              {t("orderDetail.paymentCancelMessage")}
            </p>
            <Button
              type="button"
              variant="default"
              className="shrink-0 bg-orange-600 text-white hover:bg-orange-700 dark:bg-orange-600 dark:hover:bg-orange-500"
              disabled={retryCheckoutLoading || order.status === "CANCELLED" || order.status === "PAID"}
              onClick={handleRetryCheckout}
            >
              {retryCheckoutLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t("orderDetail.paymentRetrying")}
                </>
              ) : (
                t("orderDetail.paymentRetry")
              )}
            </Button>
          </div>
        )}

        {order.status !== "CANCELLED" ? (
          <div className="rounded-xl border border-border bg-card p-6 shadow-card">
            <h3 className="mb-4 font-display text-sm font-semibold text-foreground">{t("orderDetail.tracking")}</h3>
            <OrderTrackingStepper order={order} />
          </div>
        ) : (
          <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-6">
            <div className="flex items-center gap-3">
              <XCircle className="h-6 w-6 text-destructive" />
              <div>
                <h3 className="font-display font-semibold text-destructive">{t("orderDetail.cancelledTitle")}</h3>
                <p className="text-sm text-muted-foreground">{t("orderDetail.cancelledSub")}</p>
              </div>
            </div>
          </div>
        )}

        {showOnSitePaymentQr && (
          <Card className="border-primary/30 bg-gradient-to-br from-primary/[0.07] via-card to-muted/20 shadow-md ring-1 ring-primary/10">
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <QrCode className="h-5 w-5" aria-hidden />
                </span>
                <CardTitle className="font-display text-lg tracking-tight">{t("orderDetail.paymentQrTitle")}</CardTitle>
              </div>
              <CardDescription className="pt-1 text-sm leading-relaxed text-muted-foreground">
                {t("orderDetail.paymentQrHint")}
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between sm:gap-6">
              <div className="flex justify-center sm:justify-start">
                <img
                  src={paymentQrSrc}
                  width={150}
                  height={150}
                  alt={t("orderDetail.paymentQrTitle")}
                  className="rounded-xl border border-border bg-white p-2 shadow-sm dark:bg-white"
                />
              </div>
              <div className="flex flex-col gap-2 sm:min-w-[11rem]">
                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-2 border-primary/25 bg-background/80 hover:bg-primary/5"
                  onClick={() => handlePrintPaymentQr(paymentQrSrc, orderRef)}
                >
                  <Printer className="h-4 w-4" aria-hidden />
                  {t("orderDetail.paymentQrPrint")}
                </Button>
                <Button type="button" variant="outline" className="w-full gap-2" asChild>
                  <a href={paymentQrSrc} target="_blank" rel="noopener noreferrer">
                    <ExternalLink className="h-4 w-4" aria-hidden />
                    {t("orderDetail.paymentQrOpenImage")}
                  </a>
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link to="/dashboard/orders">
              <Button variant="outline" size="icon">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <div>
              <h1 className="font-display text-xl font-bold text-foreground">CMD-{String(order.id).padStart(3, "0")}</h1>
              <p className="text-sm text-muted-foreground">
                {t("orderDetail.createdAt", { date: formatDate(order.createdAt) })}
              </p>
              <p className="text-xs text-muted-foreground">
                Paiement : {order.paymentStatus}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${badgeClass}`}>
              {t(`status.${order.status}`, { defaultValue: order.status })}
            </span>
            <Button
              variant="outline"
              className="gap-2"
              disabled={!canInvoicePdf || isDownloading}
              title={!canInvoicePdf ? t("orderDetail.invoicePaidOnly") : undefined}
              onClick={handleInvoiceDownload}
            >
              {isDownloading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
              {t("orderDetail.invoice")}
            </Button>
          </div>
        </div>

        {order.paymentStatus?.toUpperCase() === "REFUNDED" ? (
          <div className="rounded-xl border border-destructive/25 bg-muted/50 p-5 shadow-sm dark:border-destructive/30 dark:bg-destructive/10">
            <h3 className="mb-3 font-display text-sm font-semibold text-foreground">{t("orderDetail.refundInfoTitle")}</h3>
            <dl className="space-y-2 text-sm">
              <div className="flex flex-col gap-0.5 sm:flex-row sm:justify-between">
                <dt className="text-muted-foreground">{t("orderDetail.refundDateLabel")}</dt>
                <dd className="font-medium text-foreground">{formatDate(order.refundDate ?? null)}</dd>
              </div>
              <div className="flex flex-col gap-0.5 sm:flex-row sm:justify-between">
                <dt className="text-muted-foreground">{t("orderDetail.refundMethodLabel")}</dt>
                <dd className="font-medium text-foreground">
                  {order.refundMethod === "STRIPE_API"
                    ? t("orderDetail.refundMethodStripeApi")
                    : order.refundMethod === "MANUAL_CASH"
                      ? t("orderDetail.refundMethodManualCash")
                      : order.refundMethod === "MANUAL_POS"
                        ? t("orderDetail.refundMethodManualPos")
                        : order.refundMethod ?? "—"}
                </dd>
              </div>
              <div className="flex flex-col gap-0.5 sm:flex-row sm:justify-between">
                <dt className="text-muted-foreground">{t("orderDetail.refundReferenceLabel")}</dt>
                <dd className="break-all font-mono text-xs font-medium text-foreground">{order.refundReference ?? "—"}</dd>
              </div>
            </dl>
          </div>
        ) : null}

        <div className="rounded-xl border border-border bg-card p-6 shadow-card">
          <h3 className="mb-4 font-display text-sm font-semibold text-foreground">{t("orderDetail.itemsTitle")}</h3>
          <div className="space-y-3">
            {order.items.map((item, idx) => (
              <div key={`${item.serviceName}-${idx}`} className="flex items-center justify-between rounded-lg border border-border p-3">
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {item.serviceName != null && item.serviceName !== "" ? tCatalog(t, item.serviceName) : "—"}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {t("orders.quantity")} : {item.quantity}
                  </p>
                </div>
                <span className="text-sm font-semibold text-foreground">{orderLineSubtotal(item).toFixed(2)}€</span>
              </div>
            ))}
          </div>
          <div className="mt-4 flex items-center justify-between border-t border-border pt-4 font-display">
            <span className="font-semibold text-foreground">{t("orders.estimatedTotal")}</span>
            <span className="text-lg font-bold text-foreground">{subtotal.toFixed(2)}€</span>
          </div>
          {order.totalPrice != null && (
            <div className="flex items-center justify-between pt-1 font-display">
              <span className="font-semibold text-foreground">{t("orders.finalTotal")}</span>
              <span className="text-lg font-bold text-accent">{total.toFixed(2)}€</span>
            </div>
          )}
        </div>

        {order.instructions && (
          <div className="rounded-xl border border-border bg-card p-6 shadow-card">
            <h3 className="mb-2 font-display text-sm font-semibold text-foreground">{t("orderDetail.instructionsTitle")}</h3>
            <p className="text-sm text-muted-foreground">{order.instructions}</p>
          </div>
        )}
      </div>
    </ClientLayout>
  );
};

export default OrderDetailPage;
