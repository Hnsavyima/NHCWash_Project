package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.enumerations.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDTO {

    @NotNull
    private OrderStatus status;
}
