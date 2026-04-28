package com.nhcwash.backend.models.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentRescheduleDTO {

    @NotNull
    private LocalDate newDate;

    @NotNull
    private Long newTimeSlotId;
}
