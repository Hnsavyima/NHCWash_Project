package com.nhcwash.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.Appointment;
import com.nhcwash.backend.models.enumerations.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByOrder_OrderId(Long orderId);

    long countByAddress_AddressId(Long addressId);

    long countBySlot_SlotIdAndStatusNot(Long slotId, AppointmentStatus status);
}
