package com.nhcwash.backend.models.dtos;

import lombok.Data;

/** Body for staff refund endpoints — optional note for manual (on-site) refunds. */
@Data
public class OrderRefundRequestDto {

    /** Shown as {@code refundReference} for cash/POS refunds (e.g. handed back in shop). */
    private String manualNote;
}
