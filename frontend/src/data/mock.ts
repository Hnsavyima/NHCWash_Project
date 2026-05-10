/**
 * Mock data — only used by BackOfficeDashboard until /api/admin/* exists.
 * TODO: replace with admin API (list all orders, KPIs, staff actions).
 */
import type { OrderDto, ServiceDto, User } from "@/types";

export const mockUser: User = {
  id: 1,
  email: "jean.dupont@email.be",
  firstName: "Jean",
  lastName: "Dupont",
  phone: "+32 470 12 34 56",
  roles: ["ROLE_CLIENT"],
};

export const mockServices: ServiceDto[] = [
  {
    id: 1,
    name: "Lavage & Repassage",
    description: "Lavage complet avec repassage professionnel.",
    price: 3.0,
  },
  {
    id: 2,
    name: "Nettoyage à sec",
    description: "Traitement délicat pour costumes et textiles fragiles.",
    price: 8.0,
  },
];

export const mockOrders: OrderDto[] = [
  {
    id: 1,
    status: "PROCESSING",
    instructions: "Attention chemise fragile",
    totalPrice: 24.0,
    createdAt: "2026-03-18T10:30:00Z",
    paymentStatus: "PENDING",
    items: [
      { serviceName: "Lavage & Repassage", quantity: 5, unitPrice: 3.0 },
      { serviceName: "Nettoyage à sec", quantity: 1, unitPrice: 8.0 },
    ],
  },
  {
    id: 2,
    status: "DELIVERED",
    instructions: "",
    totalPrice: 32.0,
    createdAt: "2026-03-15T14:00:00Z",
    paymentStatus: "SUCCEEDED",
    items: [{ serviceName: "Nettoyage à sec", quantity: 4, unitPrice: 8.0 }],
  },
];
