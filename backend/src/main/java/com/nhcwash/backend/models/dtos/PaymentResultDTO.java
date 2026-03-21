package com.nhcwash.backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultDTO {
    private PaymentDTO payment;
    private InvoiceDTO invoice;
}
