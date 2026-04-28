package com.nhcwash.backend.models.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import com.nhcwash.backend.models.enumerations.SlotType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimeSlotBatchRequest {

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    @Min(1)
    @Max(24 * 60)
    private Integer intervalMinutes;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotNull
    private SlotType type;
}
