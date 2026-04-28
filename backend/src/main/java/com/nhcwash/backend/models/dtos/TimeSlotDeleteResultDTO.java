package com.nhcwash.backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDeleteResultDTO {
    /** {@code true} if the row was removed from the database. */
    private boolean hardDeleted;
    /** Present when the slot was only deactivated (bookings exist). */
    private TimeSlotDTO slot;
}
