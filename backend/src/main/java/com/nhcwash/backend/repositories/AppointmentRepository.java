package com.nhcwash.backend.repositories;

import com.nhcwash.backend.models.entities.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByOrder_OrderId(Long orderId);
}
