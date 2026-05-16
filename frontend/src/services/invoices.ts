import { apiRequest } from "@/lib/api";
import type { InvoiceDto } from "@/types";

export async function getInvoiceById(id: number): Promise<InvoiceDto> {
  return apiRequest<InvoiceDto>(`/invoices/${id}`);
}
