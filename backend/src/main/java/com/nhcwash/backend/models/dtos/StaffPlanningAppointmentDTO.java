package com.nhcwash.backend.models.dtos;

import lombok.Data;

@Data
public class StaffPlanningAppointmentDTO {
    private Long id;
    private Long orderId;
    private String appointmentType;
    private String status;
    private String slotStartAt;
    private String slotEndAt;
    private String slotType;
    private String clientName;
    private String addressSummary;
}
