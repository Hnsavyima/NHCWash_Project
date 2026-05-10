// ===== Aligned with Spring Boot backend DTOs =====

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  /** Persisted locale: FR, EN, NL */
  preferredLanguage?: string | null;
  /** Server path such as `/uploads/avatars/{uuid}.jpg` */
  avatarUrl?: string | null;
  roles: string[];
}

export interface UserUpdatePayload {
  firstName: string;
  lastName: string;
  phone?: string;
  preferredLanguage?: string;
}

/** PATCH /api/users/me — omitted fields are left unchanged. */
export interface UserProfilePatchPayload {
  firstName?: string;
  lastName?: string;
  phone?: string | null;
  preferredLanguage?: string;
}

/** GET /api/services — ServiceDTO */
export interface ServiceDto {
  id: number;
  name: string;
  description: string | null;
  price: number | null;
  /** Present on admin catalogue; public list only returns active services. */
  active?: boolean | null;
  /** Admin catalogue: category FK for forms. */
  categoryId?: number | null;
  estimatedDelayHours?: number | null;
}

/** GET /api/admin/services/meta/categories */
export interface ServiceCategoryDto {
  id: number;
  name: string;
}

/** POST/PUT /api/admin/services — ServiceUpsertRequest */
export interface ServiceUpsertPayload {
  name: string;
  description?: string | null;
  basePrice: number;
  categoryId: number;
  estimatedDelayHours?: number | null;
  active?: boolean | null;
}

export function serviceUnitPrice(s: ServiceDto): number {
  return s.price ?? 0;
}

/** GET/POST /api/addresses — UserAddressDTO */
export interface UserAddress {
  id: number;
  label?: string | null;
  street: string;
  number?: string | null;
  box?: string | null;
  postalCode?: string | null;
  city: string;
  country: string;
  defaultAddress: boolean;
}

/** Display helper: backend uses postalCode */
export function formatAddressLine(addr: UserAddress): string {
  const line2 = [addr.postalCode, addr.city].filter(Boolean).join(" ");
  return [addr.street, line2, addr.country].filter(Boolean).join(", ");
}

export interface UserAddressCreatePayload {
  label?: string;
  street: string;
  number?: string;
  box?: string;
  postalCode?: string;
  city: string;
  country: string;
  defaultAddress?: boolean;
}

/** GET /api/orders — OrderDTO */
export interface OrderItemDto {
  serviceName: string;
  quantity: number;
  unitPrice: number | null;
}

/** Public reference in emails / QR (matches backend `EmailTemplateService.formatOrderRef`). */
export function orderPublicReference(orderId: number): string {
  return `CMD-${String(orderId).padStart(3, "0")}`;
}

export interface OrderDto {
  id: number;
  status: string;
  totalPrice: number | null;
  createdAt: string | null;
  instructions: string | null;
  items: OrderItemDto[];
  paymentStatus: string;
  checkoutPaymentMode?: string | null;
  /** Succeeded payment method when known (e.g. CARD, CASH, POS_TERMINAL). */
  paymentMethod?: string | null;
  /** Staff listing from GET /api/employee/orders */
  clientFirstName?: string | null;
  clientLastName?: string | null;
  clientEmail?: string | null;
  /** Staff: display name from API (first + last, or email). */
  clientName?: string | null;
  /** When payment was refunded (ISO string from API). */
  refundDate?: string | null;
  /** STRIPE_API | MANUAL_CASH | MANUAL_POS */
  refundMethod?: string | null;
  /** Stripe refund id or manual note. */
  refundReference?: string | null;
}

export function orderLineSubtotal(item: OrderItemDto): number {
  const u = item.unitPrice ?? 0;
  return u * item.quantity;
}

export function orderComputedSubtotal(order: OrderDto): number {
  return order.items.reduce((sum, i) => sum + orderLineSubtotal(i), 0);
}

export function orderDisplayTotal(order: OrderDto): number {
  if (order.totalPrice != null) return order.totalPrice;
  return orderComputedSubtotal(order);
}

/** Staff / backoffice: client label for tables (uses API `clientName` or first/last/email). */
export function orderClientDisplayName(order: OrderDto): string {
  const fromApi = order.clientName?.trim();
  if (fromApi) return fromApi;
  const joined = [order.clientFirstName, order.clientLastName].filter(Boolean).join(" ").trim();
  if (joined) return joined;
  const email = order.clientEmail?.trim();
  if (email) return email;
  return "—";
}

/**
 * Builds a comma-separated list of service labels for an order.
 * Pass `translate` (e.g. `(n) => tCatalog(t, n)`) so each name is translated before joining;
 * otherwise raw API names are joined.
 */
export function orderServiceSummary(
  order: OrderDto,
  translateServiceName: (raw: string) => string = (s) => s
): string {
  return order.items
    .map((i) => translateServiceName(i.serviceName ?? ""))
    .filter((s) => String(s).trim() !== "")
    .join(", ");
}

export type CheckoutPaymentMode = "ONLINE" | "CASH_ON_SITE";

export interface CreateOrderPayload {
  items: { serviceId: number; quantity: number }[];
  instructions?: string;
  /** Default when omitted: ONLINE (Stripe). */
  checkoutPaymentMode?: CheckoutPaymentMode;
}

export type OrderStatusKey =
  | "PENDING"
  | "RECEIVED"
  | "PAID"
  | "PROCESSING"
  | "READY"
  | "DELIVERED"
  | "CANCELLED";

export const ORDER_STATUS_CONFIG: Record<string, { label: string; color: string }> = {
  PENDING: { label: "En attente", color: "bg-warning/10 text-warning" },
  RECEIVED: { label: "Reçue", color: "bg-warning/10 text-warning" },
  PAID: { label: "Payée", color: "bg-success/10 text-success" },
  PROCESSING: { label: "En traitement", color: "bg-info/10 text-info" },
  READY: { label: "Prête", color: "bg-info/10 text-info" },
  DELIVERED: { label: "Livrée", color: "bg-success/10 text-success" },
  CANCELLED: { label: "Annulée", color: "bg-destructive/10 text-destructive" },
  REFUNDED: { label: "Remboursée", color: "bg-muted text-muted-foreground" },
};

export function statusBadgeClass(status: string): string {
  const color = ORDER_STATUS_CONFIG[status]?.color ?? "bg-muted text-muted-foreground";
  return `${color} inline-flex items-center whitespace-nowrap`;
}

/** GET /api/timeslots — TimeSlotDTO */
export interface TimeSlotDto {
  id: number;
  slotType: "PICKUP" | "DELIVERY";
  startAt: string;
  endAt: string;
  capacityMax: number;
  remainingCapacity: number | null;
  active: boolean | null;
}

/** DELETE /api/admin/timeslots/{id} */
export interface TimeSlotDeleteResultDto {
  hardDeleted: boolean;
  slot: TimeSlotDto | null;
}

export interface AppointmentDto {
  id: number;
  orderId: number;
  timeSlotId: number;
  addressId: number;
  appointmentType: "PICKUP" | "DELIVERY";
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

/** POST body uses `type` (see AppointmentBookingRequestDTO) */
export interface AppointmentBookingPayload {
  orderId: number;
  timeSlotId: number;
  addressId: number;
  type: "PICKUP" | "DELIVERY";
}

export type PaymentProvider = "STRIPE" | "PAYPAL" | "ONSITE";
export type PaymentMethod = "CARD" | "CASH" | "POS_TERMINAL";

export interface PaymentRequestPayload {
  orderId: number;
  provider: PaymentProvider;
  method: PaymentMethod;
  currency?: string;
}

/** {@code POST /api/payments/order/{id}} — open {@link CheckoutSessionResponse.url} in the browser. */
export interface CheckoutSessionResponse {
  url: string;
}

export interface PaymentDto {
  id: number;
  orderId: number;
  amount: number;
  currency: string;
  provider: PaymentProvider;
  method: PaymentMethod;
  status: string;
  providerTxId?: string | null;
  paidAt?: string | null;
  createdAt?: string | null;
}

export interface InvoiceLineDto {
  id: number;
  label: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface InvoiceDto {
  id: number;
  orderId: number;
  invoiceNumber: string;
  issuedAt: string;
  vatRate: number;
  subtotal: number;
  vatAmount: number;
  total: number;
  pdfPath?: string | null;
  lines: InvoiceLineDto[];
}

export interface PaymentResultDto {
  payment: PaymentDto;
  invoice: InvoiceDto;
}

/** GET /api/public/settings & /api/admin/settings */
export interface GlobalSettingsDto {
  companyName: string;
  contactEmail: string;
  contactPhone: string;
  address: string;
  vatNumber: string;
  openingHoursDescription: string;
  supportEmail: string;
}

export interface ApiKeyListItemDto {
  id: number;
  name: string;
  maskedKey: string;
  createdAt: string | null;
  active: boolean;
}

export interface CreateApiKeyResponseDto {
  id: number;
  name: string;
  key: string;
  createdAt: string | null;
}
