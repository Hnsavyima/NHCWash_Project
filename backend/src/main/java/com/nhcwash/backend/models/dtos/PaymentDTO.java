package com.nhcwash.backend.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.enumerations.PaymentProvider;
import com.nhcwash.backend.models.enumerations.PaymentStatus;

import lombok.Data;

@Data
public class PaymentDTO {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private PaymentProvider provider;
    private PaymentMethod method;
    private PaymentStatus status;
    private String providerTxId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
