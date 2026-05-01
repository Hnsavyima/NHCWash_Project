package com.nhcwash.backend.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.TimeSlotDTO;
import com.nhcwash.backend.models.entities.TimeSlot;
import com.nhcwash.backend.models.enumerations.AppointmentStatus;
import com.nhcwash.backend.models.enumerations.SlotType;
import com.nhcwash.backend.repositories.AppointmentRepository;
import com.nhcwash.backend.repositories.TimeSlotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final DtoConverter dtoConverter;

    @Transactional(readOnly = true)
    public List<TimeSlotDTO> listActiveSlots(Optional<SlotType> typeFilter) {
        List<TimeSlot> slots = typeFilter
                .map(t -> timeSlotRepository.findByIsActiveTrueAndSlotTypeOrderByStartAtAsc(t))
                .orElseGet(() -> timeSlotRepository.findByIsActiveTrueOrderByStartAtAsc());

        return slots.stream().map(this::toDtoWithRemaining).collect(Collectors.toList());
    }

    /** Active slots whose {@link TimeSlot#getStartAt()} falls on {@code date} (local calendar day). */
    @Transactional(readOnly = true)
    public List<TimeSlotDTO> listActiveSlotsForDate(LocalDate date, Optional<SlotType> typeFilter) {
        if (date == null) {
            return listActiveSlots(typeFilter);
        }
        List<TimeSlot> base = typeFilter
                .map(t -> timeSlotRepository.findByIsActiveTrueAndSlotTypeOrderByStartAtAsc(t))
                .orElseGet(() -> timeSlotRepository.findByIsActiveTrueOrderByStartAtAsc());
        return base.stream()
                .filter(s -> s.getStartAt() != null && date.equals(s.getStartAt().toLocalDate()))
                .map(this::toDtoWithRemaining)
                .collect(Collectors.toList());
    }

    private TimeSlotDTO toDtoWithRemaining(TimeSlot slot) {
        long booked = appointmentRepository.countBySlot_SlotIdAndStatusNot(slot.getSlotId(), AppointmentStatus.CANCELLED);
        int remaining = Math.max(0, slot.getCapacityMax() - (int) booked);
        return dtoConverter.toTimeSlotDto(slot, remaining);
    }
}
