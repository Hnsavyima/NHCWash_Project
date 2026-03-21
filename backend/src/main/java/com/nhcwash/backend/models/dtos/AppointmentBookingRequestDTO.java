package com.nhcwash.backend.models.dtos;

import com.nhcwash.backend.models.enumerations.AppointmentType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentBookingRequestDTO {

    @NotNull
    private Long orderId;

    @NotNull
    private Long timeSlotId;

    @NotNull
    private Long addressId;

    @NotNull
    private AppointmentType type;
}
