import { apiRequest, apiRequestBlob } from "@/lib/api";
import type { CreateOrderPayload, OrderDto } from "@/types";

export async function getOrders(): Promise<OrderDto[]> {
  return apiRequest<OrderDto[]>("/orders");
}

/** All orders — ADMIN / EMPLOYEE only (`GET /api/employee/orders`). */
export async function getStaffOrders(): Promise<OrderDto[]> {
  return apiRequest<OrderDto[]>("/employee/orders");
}

/** Staff: PATCH order lifecycle status (`ROLE_ADMIN` / `ROLE_EMPLOYEE`). */
export async function updateEmployeeOrderStatus(orderId: number, status: string): Promise<OrderDto> {
  return apiRequest<OrderDto>(`/employee/orders/${orderId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export async function getOrderById(id: number): Promise<OrderDto> {
  return apiRequest<OrderDto>(`/orders/${id}`);
}

/** Single order for staff (`GET /api/employee/orders/:id`). */
export async function getStaffOrderById(id: number): Promise<OrderDto> {
  return apiRequest<OrderDto>(`/employee/orders/${id}`);
}

/** Staff: resolve `CMD-018` or numeric id (`GET /api/employee/orders/by-reference/:ref`). */
export async function getStaffOrderByReference(ref: string): Promise<OrderDto> {
  const encoded = encodeURIComponent(ref.trim());
  return apiRequest<OrderDto>(`/employee/orders/by-reference/${encoded}`);
}

/**
 * Counter payment — cash or physical terminal (`POST /api/employee/orders/:id/pay-manual`).
 * Body: `{ method: "CASH" | "POS_TERMINAL" }` (Spring enum names).
 */
export async function markOrderAsPaidManually(orderId: number, method: string): Promise<OrderDto> {
  return apiRequest<OrderDto>(`/employee/orders/${orderId}/pay-manual`, {
    method: "POST",
    body: JSON.stringify({ method }),
  });
}

/** Admin / staff: full refund (`POST /api/admin/orders/:id/refund` — same rules as mark-as-paid). */
export async function postStaffOrderRefund(orderId: number, manualNote?: string | null): Promise<OrderDto> {
  return apiRequest<OrderDto>(`/admin/orders/${orderId}/refund`, {
    method: "POST",
    body: JSON.stringify(manualNote != null && manualNote !== "" ? { manualNote } : {}),
  });
}

/** Admin / staff: manual mark as paid (`POST /api/admin/orders/:id/mark-as-paid`). */
export async function postAdminMarkOrderAsPaid(
  orderId: number,
  method: "CASH" | "POS_TERMINAL" = "CASH"
): Promise<OrderDto> {
  return apiRequest<OrderDto>(`/admin/orders/${orderId}/mark-as-paid`, {
    method: "POST",
    body: JSON.stringify({ method }),
  });
}

/** PDF facture — `GET /api/orders/:id/invoice` (commande réglée : paiement réussi ou statut payée/livrée). */
export async function downloadOrderInvoicePdf(orderId: number): Promise<Blob> {
  return apiRequestBlob(`/orders/${orderId}/invoice`);
}

export async function createOrder(payload: CreateOrderPayload): Promise<OrderDto> {
  return apiRequest<OrderDto>("/orders", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
