package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.enumerations.PaymentMethod;

import lombok.Data;

/** Optional body for {@code POST /api/admin/orders/{id}/mark-as-paid}. Defaults to {@link PaymentMethod#CASH}. */
@Data
public class MarkAsPaidRequestDto {

    private PaymentMethod method;
}
