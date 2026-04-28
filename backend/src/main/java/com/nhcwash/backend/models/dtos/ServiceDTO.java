package com.nhcwash.backend.models.dtos;

import lombok.Data;

@Data
public class ServiceDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    /** Whether the service is offered on the public catalogue. */
    private Boolean active;
    /** Admin catalogue: owning category. */
    private Long categoryId;
    /** Estimated turnaround in hours (optional display / forms). */
    private Integer estimatedDelayHours;
}
