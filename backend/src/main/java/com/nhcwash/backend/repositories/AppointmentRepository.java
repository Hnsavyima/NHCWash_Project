package com.nhcwash.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhcwash.backend.models.entities.Appointment;
import com.nhcwash.backend.models.enumerations.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByOrder_OrderId(Long orderId);

    long countByAddress_AddressId(Long addressId);

    long countBySlot_SlotIdAndStatusNot(Long slotId, AppointmentStatus status);

    @EntityGraph(attributePaths = {"slot", "order", "address"})
    @Query("SELECT a FROM Appointment a WHERE a.order.client.userId = :userId ORDER BY a.createdAt DESC")
    List<Appointment> findWithDetailsByClientUserId(@Param("userId") Long userId);
}
