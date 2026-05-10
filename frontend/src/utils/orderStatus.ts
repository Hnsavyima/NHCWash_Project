/**
 * Normalize API / i18n status strings for reliable comparisons (casing, accents).
 */
export function normalizeOrderStatus(status: string | null | undefined): string {
  const s = String(status ?? "")
    .trim()
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toUpperCase()
    .replace(/\s+/g, "_");
  if (s === "PAYEE") return "PAID";
  if (s === "COMPLETE" || s === "COMPLETED") return "DELIVERED";
  return s;
}

export function isOrderActiveClient(status: string | null | undefined): boolean {
  const s = normalizeOrderStatus(status);
  return s !== "DELIVERED" && s !== "CANCELLED";
}

export function isOrderPendingClient(status: string | null | undefined): boolean {
  const s = normalizeOrderStatus(status);
  return s === "PENDING" || s === "RECEIVED";
}

export function countsTowardTotalSpent(order: { status: string; paymentStatus?: string | null }): boolean {
  const st = normalizeOrderStatus(order.status);
  const ps = normalizeOrderStatus(order.paymentStatus);
  return ["PAID", "DELIVERED"].includes(st) || ps === "SUCCEEDED";
}

export function isOrderCompletedAdmin(status: string | null | undefined): boolean {
  const s = normalizeOrderStatus(status);
  return s === "DELIVERED" || s === "PAID";
}

export function isPaidForRevenue(order: { status: string; paymentStatus?: string | null }): boolean {
  const st = normalizeOrderStatus(order.status);
  const ps = normalizeOrderStatus(order.paymentStatus);
  return st === "PAID" || st === "DELIVERED" || ps === "SUCCEEDED";
}

export function needsStaffAction(status: string | null | undefined): boolean {
  const s = normalizeOrderStatus(status);
  return ["PENDING", "RECEIVED", "PROCESSING", "PAID", "READY"].includes(s);
}

export function isPriorityQueueOrder(status: string | null | undefined): boolean {
  const s = normalizeOrderStatus(status);
  if (s === "DELIVERED" || s === "CANCELLED") return false;
  return ["PENDING", "RECEIVED", "PROCESSING", "PAID", "READY"].includes(s);
}

/** Orders eligible for client invoice PDF (paid or fulfilled). COMPLETED normalizes to DELIVERED. */
export function isOrderInvoiceEligible(status: string | null | undefined): boolean {
  const s = normalizeOrderStatus(status);
  return s === "PAID" || s === "DELIVERED";
}

/**
 * Client may download the invoice PDF when the order was settled or refunded (invoice remains valid).
 */
export function isClientInvoiceDownloadAllowed(order: {
  status: string;
  paymentStatus?: string | null;
}): boolean {
  const ps = normalizeOrderStatus(order.paymentStatus);
  const st = normalizeOrderStatus(order.status);
  if (ps === "SUCCEEDED" || ps === "SUCCESS" || ps === "PAID" || ps === "REFUNDED") return true;
  if (st === "PAID" || st === "DELIVERED") return true;
  return false;
}
