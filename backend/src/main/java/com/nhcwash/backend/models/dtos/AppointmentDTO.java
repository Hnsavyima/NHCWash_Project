package com.nhcwash.backend.models.dtos;

import java.time.LocalDateTime;

import com.nhcwash.backend.models.enumerations.AppointmentStatus;
import com.nhcwash.backend.models.enumerations.AppointmentType;

import lombok.Data;

@Data
public class AppointmentDTO {
    private Long id;
    private Long orderId;
    private Long timeSlotId;
    private Long addressId;
    private AppointmentType appointmentType;
    private AppointmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
