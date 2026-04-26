package com.nhcwash.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StripePaymentConfirmDto {

    @NotNull
    private Long orderId;

    @NotBlank
    @JsonProperty("session_id")
    private String sessionId;
}
