package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.enumerations.PaymentProvider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentRequestDTO {

    @NotNull
    private Long orderId;

    @NotNull
    private PaymentProvider provider;

    @NotNull
    private PaymentMethod method;

    /** ISO currency code; defaults to EUR in the service if omitted. */
    @Size(min = 3, max = 10)
    private String currency;
}
