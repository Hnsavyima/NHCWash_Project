package com.nhcwash.backend.models.dtos;

/**
 * Response for {@code POST /api/payments/order/{id}} — client redirects to {@link #url()}.
 */
public record CheckoutSessionUrlDto(String url) {
}
