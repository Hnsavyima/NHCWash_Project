package com.nhcwash.backend.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private String status;
    private Double totalPrice;
    private String createdAt;
    private String instructions;
    private List<OrderItemDTO> items;
    private String paymentStatus;

    /** {@code ONLINE} or {@code CASH_ON_SITE}; omitted in JSON for legacy rows → treat as online. */
    private String checkoutPaymentMode;
    /** Payment method of the succeeded payment, when any (e.g. CARD, CASH, POS_TERMINAL). */
    private String paymentMethod;
    /** Populated for staff listings when the client is loaded on the order. */
    private String clientFirstName;
    private String clientLastName;
    private String clientEmail;
    /** Full display name for staff tables (first + last, or email if names missing). */
    private String clientName;

    /** Set when the order has been refunded (ISO-8601 local). */
    private String refundDate;
    /** e.g. STRIPE_API, MANUAL_CASH, MANUAL_POS */
    private String refundMethod;
    /** Stripe refund id or manual note. */
    private String refundReference;
}
