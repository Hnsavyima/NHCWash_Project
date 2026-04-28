package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.enumerations.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualPaymentRequest {

    /** {@link PaymentMethod#CASH} or {@link PaymentMethod#POS_TERMINAL} for counter payments. */
    @NotNull
    private PaymentMethod method;
}
