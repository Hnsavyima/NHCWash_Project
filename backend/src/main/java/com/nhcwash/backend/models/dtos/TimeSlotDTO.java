package com.nhcwash.backend.models.dtos;

import java.time.LocalDateTime;

import com.nhcwash.backend.models.enumerations.SlotType;

import lombok.Data;

@Data
public class TimeSlotDTO {
    private Long id;
    private SlotType slotType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer capacityMax;
    /** Remaining places (capacity minus non-cancelled bookings). */
    private Integer remainingCapacity;
    private Boolean active;
}
