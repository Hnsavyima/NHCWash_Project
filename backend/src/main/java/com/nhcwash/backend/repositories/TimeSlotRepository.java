package com.nhcwash.backend.repositories;

import com.nhcwash.backend.models.entities.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
}
