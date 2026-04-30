package com.nhcwash.backend.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.TimeSlotBatchRequest;
import com.nhcwash.backend.models.dtos.TimeSlotDTO;
import com.nhcwash.backend.models.dtos.TimeSlotDeleteResultDTO;
import com.nhcwash.backend.models.dtos.TimeSlotUpdateRequest;
import com.nhcwash.backend.models.entities.TimeSlot;
import com.nhcwash.backend.models.enumerations.AppointmentStatus;
import com.nhcwash.backend.repositories.AppointmentRepository;
import com.nhcwash.backend.repositories.TimeSlotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminTimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final DtoConverter dtoConverter;

    @Transactional(readOnly = true)
    public List<TimeSlotDTO> listAllOrderedByStart() {
        return timeSlotRepository.findAllByOrderByStartAtAsc().stream()
                .map(this::toDtoWithRemaining)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TimeSlotDTO> generateBatch(TimeSlotBatchRequest req) {
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de fin doit être après l'heure de début");
        }
        int interval = req.getIntervalMinutes();
        if (interval <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'intervalle doit être positif");
        }
        LocalDateTime cursor = LocalDateTime.of(req.getDate(), req.getStartTime());
        LocalDateTime endBoundary = LocalDateTime.of(req.getDate(), req.getEndTime());
        if (!endBoundary.isAfter(cursor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plage horaire invalide");
        }

        List<TimeSlotDTO> created = new ArrayList<>();
        while (cursor.isBefore(endBoundary)) {
            LocalDateTime slotEnd = cursor.plusMinutes(interval);
            if (slotEnd.isAfter(endBoundary)) {
                slotEnd = endBoundary;
            }
            if (!slotEnd.isAfter(cursor)) {
                break;
            }
            TimeSlot slot = new TimeSlot();
            slot.setSlotType(req.getType());
            slot.setStartAt(cursor);
            slot.setEndAt(slotEnd);
            slot.setCapacityMax(req.getCapacity());
            slot.setIsActive(true);
            TimeSlot saved = timeSlotRepository.save(slot);
            created.add(toDtoWithRemaining(saved));
            cursor = slotEnd;
        }
        if (created.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun créneau généré (intervalle trop grand ?)");
        }
        return created;
    }

    @Transactional
    public TimeSlotDTO update(Long slotId, TimeSlotUpdateRequest req) {
        if (req.getCapacityMax() == null && req.getActive() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rien à mettre à jour");
        }
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        if (req.getCapacityMax() != null) {
            slot.setCapacityMax(req.getCapacityMax());
        }
        if (req.getActive() != null) {
            slot.setIsActive(req.getActive());
        }
        TimeSlot saved = timeSlotRepository.save(slot);
        return toDtoWithRemaining(saved);
    }

    @Transactional
    public TimeSlotDeleteResultDTO deleteOrDeactivate(Long slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));
        long linkedAppointments = appointmentRepository.countBySlot_SlotId(slotId);
        if (linkedAppointments > 0) {
            slot.setIsActive(false);
            TimeSlot saved = timeSlotRepository.save(slot);
            return new TimeSlotDeleteResultDTO(false, toDtoWithRemaining(saved));
        }
        timeSlotRepository.delete(slot);
        return new TimeSlotDeleteResultDTO(true, null);
    }

    private TimeSlotDTO toDtoWithRemaining(TimeSlot slot) {
        long booked = appointmentRepository.countBySlot_SlotIdAndStatusNot(slot.getSlotId(), AppointmentStatus.CANCELLED);
        int remaining = Math.max(0, slot.getCapacityMax() - (int) booked);
        return dtoConverter.toTimeSlotDto(slot, remaining);
    }
}
