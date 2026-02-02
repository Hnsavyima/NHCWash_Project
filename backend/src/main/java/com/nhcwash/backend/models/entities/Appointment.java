package com.nhcwash.backend.models.entities;

import com.nhcwash.backend.models.enumerations.AppointmentStatus;
import com.nhcwash.backend.models.enumerations.AppointmentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private UserAddress address;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false, length = 20)
    private AppointmentType appointmentType; // PICKUP | DELIVERY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status; // BOOKED | CONFIRMED | RESCHEDULED | CANCELLED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
