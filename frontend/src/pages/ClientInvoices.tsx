import { useState, useCallback, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Download, FileText, Loader2 } from "lucide-react";
import ClientLayout from "@/components/layouts/ClientLayout";
import { Button } from "@/components/ui/button";
import { useQuery } from "@tanstack/react-query";
import { tCatalog } from "@/i18n/catalog";
import { orderDisplayTotal, orderServiceSummary, statusBadgeClass, type OrderDto } from "@/types";
import { isClientInvoiceDownloadAllowed, isOrderInvoiceEligible } from "@/utils/orderStatus";
import { getOrders, downloadOrderInvoicePdf } from "@/services/orders";
import { toast } from "@/components/ui/sonner";
import { Pagination } from "@/components/ui/pagination";
import { ApiError } from "@/lib/api";
import { dateLocaleFromI18n } from "@/lib/dateLocaleFromI18n";

const ClientInvoices = () => {
  const { t, i18n } = useTranslation();
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState(1);

  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ["orders"],
    queryFn: getOrders,
    refetchOnWindowFocus: true,
  });

  const sourceData = useMemo(
    () =>
      orders
        .filter((o) => isOrderInvoiceEligible(o.status))
        .sort((a, b) => {
          const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return tb - ta;
        }),
    [orders],
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [orders]);

  const ITEMS_PER_PAGE = 10;
  const totalPages = Math.ceil(sourceData.length / ITEMS_PER_PAGE);
  const page = Math.max(1, Math.min(currentPage, totalPages || 1));
  const paginatedData = sourceData.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

  const handleDownload = useCallback(
    async (order: OrderDto) => {
      if (!isClientInvoiceDownloadAllowed(order) || downloadingId != null) return;
      setDownloadingId(order.id);
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
        setDownloadingId(null);
      }
    },
    [downloadingId, t]
  );

  const formatDate = (iso: string | null) =>
    iso
      ? new Date(iso).toLocaleDateString(dateLocaleFromI18n(i18n.language), {
          day: "numeric",
          month: "short",
          year: "numeric",
        })
      : "—";

  return (
    <ClientLayout>
      <div className="space-y-6">
        <div>
          <h1 className="font-display text-2xl font-bold text-foreground">{t("invoices.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("invoices.subtitle")}</p>
        </div>

        {error && (
          <p className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {(error as Error).message}
          </p>
        )}

        {isLoading && <div className="text-sm text-muted-foreground">…</div>}

        {!isLoading && sourceData.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-border bg-card p-12 text-center shadow-card">
            <FileText className="h-12 w-12 text-muted-foreground/50" />
            <h3 className="mt-4 font-display text-lg font-semibold text-foreground">{t("invoices.noInvoices")}</h3>
            <p className="mt-1 max-w-md text-sm text-muted-foreground">{t("invoices.noInvoicesSub")}</p>
          </div>
        ) : null}

        {!isLoading && sourceData.length > 0 ? (
          <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-border bg-muted/40 text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    <th className="px-6 py-3">{t("invoices.colOrder")}</th>
                    <th className="px-6 py-3">{t("invoices.colServices")}</th>
                    <th className="px-6 py-3">{t("invoices.colDate")}</th>
                    <th className="px-6 py-3">{t("invoices.colStatus")}</th>
                    <th className="px-6 py-3 text-right">{t("invoices.colTotal")}</th>
                    <th className="px-6 py-3 text-right">{t("invoices.colAction")}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {paginatedData.map((order) => {
                    const badgeClass = statusBadgeClass(order.status);
                    const busy = downloadingId === order.id;
                    const canPdf = isClientInvoiceDownloadAllowed(order);
                    return (
                      <tr key={order.id} className="transition-colors hover:bg-muted/40">
                        <td className="px-6 py-4 font-medium text-foreground">
                          CMD-{String(order.id).padStart(3, "0")}
                        </td>
                        <td className="max-w-[220px] truncate px-6 py-4 text-muted-foreground">
                          {orderServiceSummary(order, (name) => tCatalog(t, name))}
                        </td>
                        <td className="px-6 py-4 text-muted-foreground">{formatDate(order.createdAt)}</td>
                        <td className="px-6 py-4">
                          <span className={`rounded-full px-3 py-1 text-xs font-medium ${badgeClass}`}>
                            {t(`status.${order.status}`, { defaultValue: order.status })}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right font-semibold text-foreground">
                          {orderDisplayTotal(order).toFixed(2)}€
                        </td>
                        <td className="px-6 py-4 text-right">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="gap-2"
                            disabled={!canPdf || downloadingId != null}
                            title={!canPdf ? t("orderDetail.invoicePaidOnly") : undefined}
                            onClick={() => handleDownload(order)}
                          >
                            {busy ? (
                              <Loader2 className="h-4 w-4 shrink-0 animate-spin" aria-hidden />
                            ) : (
                              <Download className="h-4 w-4 shrink-0" aria-hidden />
                            )}
                            {busy ? t("invoices.generating") : t("invoices.downloadPdf")}
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {totalPages > 1 ? (
              <div className="mt-4 border-t border-gray-100 pt-4 pb-2 dark:border-gray-800">
                <Pagination
                  className="border-t-0 bg-transparent"
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={setCurrentPage}
                />
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    </ClientLayout>
  );
};

export default ClientInvoices;
