package com.nhcwash.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.TimeSlot;
import com.nhcwash.backend.models.enumerations.SlotType;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findAllByOrderByStartAtAsc();

    List<TimeSlot> findByIsActiveTrueOrderByStartAtAsc();

    List<TimeSlot> findByIsActiveTrueAndSlotTypeOrderByStartAtAsc(SlotType slotType);
}
