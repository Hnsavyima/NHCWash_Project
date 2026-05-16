import { apiRequest } from "@/lib/api";
import type { CheckoutSessionResponse, PaymentResultDto } from "@/types";

export async function createCheckoutSession(orderId: number): Promise<CheckoutSessionResponse> {
  return apiRequest<CheckoutSessionResponse>(`/payments/order/${orderId}`, {
    method: "POST",
  });
}

export async function confirmStripePayment(payload: {
  orderId: number;
  session_id: string;
}): Promise<PaymentResultDto> {
  return apiRequest<PaymentResultDto>("/payments/confirm", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
