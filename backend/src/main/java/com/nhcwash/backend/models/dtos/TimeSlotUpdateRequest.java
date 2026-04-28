package com.nhcwash.backend.models.dtos;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TimeSlotUpdateRequest {

    @Min(1)
    private Integer capacityMax;

    private Boolean active;
}
