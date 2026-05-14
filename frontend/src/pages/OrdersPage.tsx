import { Link } from "react-router-dom";
import { Package, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Input } from "@/components/ui/input";
import ClientLayout from "@/components/layouts/ClientLayout";
import { tCatalog } from "@/i18n/catalog";
import { orderDisplayTotal, orderServiceSummary, statusBadgeClass, type OrderDto } from "@/types";
import { getOrders } from "@/services/orders";
import { dateLocaleFromI18n } from "@/lib/dateLocaleFromI18n";
import { Pagination } from "@/components/ui/pagination";

/** Row badge: refunds override fulfillment; cancelled next; otherwise fulfillment status. */
function clientOrdersRowBadgeStatus(order: OrderDto): string {
  const ps = (order.paymentStatus ?? "").toUpperCase();
  if (ps === "REFUNDED") return "REFUNDED";
  if (order.status === "CANCELLED") return "CANCELLED";
  return order.status;
}

const OrdersPage = () => {
  const { t, i18n } = useTranslation();
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const { data: orders = [], isLoading, error } = useQuery({
    queryKey: ["orders"],
    queryFn: getOrders,
    refetchOnWindowFocus: true,
  });

  const filteredOrders = useMemo(() => {
    return orders.filter((o) => {
      if (!search) return true;
      const cmd = `CMD-${String(o.id).padStart(3, "0")}`;
      return (
        cmd.toLowerCase().includes(search.toLowerCase()) ||
        String(o.id).includes(search)
      );
    });
  }, [orders, search]);

  useEffect(() => {
    setCurrentPage(1);
  }, [search]);

  const ITEMS_PER_PAGE = 10;
  const totalPages = Math.ceil(filteredOrders.length / ITEMS_PER_PAGE);
  const page = Math.max(1, Math.min(currentPage, totalPages || 1));
  const paginatedOrders = filteredOrders.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

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
          <h1 className="font-display text-2xl font-bold text-foreground">{t("orders.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("orders.subtitle")}</p>
        </div>

        {error && (
          <p className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {(error as Error).message}
          </p>
        )}

        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t("orders.searchPlaceholder")}
            className="pl-10"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {isLoading && <div className="text-sm text-muted-foreground">…</div>}

        {!isLoading && filteredOrders.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-border bg-card p-12 text-center shadow-card">
            <Package className="h-12 w-12 text-muted-foreground/50" />
            <h3 className="mt-4 font-display text-lg font-semibold text-foreground">{t("orders.noOrders")}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{t("orders.noOrdersSub")}</p>
          </div>
        ) : (
          !isLoading && (
            <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
              <div className="divide-y divide-border">
                {paginatedOrders.map((order) => {
                  const rowStatus = clientOrdersRowBadgeStatus(order);
                  const badgeClass = statusBadgeClass(rowStatus);
                  return (
                    <Link
                      key={order.id}
                      to={`/dashboard/orders/${order.id}`}
                      className="flex items-center justify-between px-6 py-4 transition-colors hover:bg-muted/50"
                    >
                      <div className="flex items-center gap-4">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/5">
                          <Package className="h-5 w-5 text-primary" />
                        </div>
                        <div>
                          <p className="text-sm font-medium text-foreground">CMD-{String(order.id).padStart(3, "0")}</p>
                          <p className="text-xs text-muted-foreground">
                            {orderServiceSummary(order, (name) => tCatalog(t, name))} • {formatDate(order.createdAt)}
                          </p>
                        </div>
                      </div>
                      <div className="flex min-w-0 flex-1 items-center justify-end gap-6">
                        <div className="shrink-0">
                          <span className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${badgeClass}`}>
                            {t(`status.${rowStatus}`, { defaultValue: rowStatus })}
                          </span>
                        </div>
                        <span className="w-24 shrink-0 text-right text-sm font-semibold tabular-nums text-foreground">
                          {orderDisplayTotal(order).toFixed(2)}€
                        </span>
                      </div>
                    </Link>
                  );
                })}
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
          )
        )}
      </div>
    </ClientLayout>
  );
};

export default OrdersPage;
