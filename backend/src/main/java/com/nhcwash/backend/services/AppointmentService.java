package com.nhcwash.backend.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.AppointmentBookingRequestDTO;
import com.nhcwash.backend.models.entities.Appointment;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.TimeSlot;
import com.nhcwash.backend.models.entities.UserAddress;
import com.nhcwash.backend.models.enumerations.AppointmentStatus;
import com.nhcwash.backend.models.enumerations.SlotType;
import com.nhcwash.backend.repositories.AppointmentRepository;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.TimeSlotRepository;
import com.nhcwash.backend.repositories.UserAddressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final OrderRepository orderRepository;
    private final UserAddressRepository userAddressRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<Appointment> listAppointmentsForClient(Long userId) {
        return appointmentRepository.findWithDetailsByClientUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> listAllForStaffPlanning() {
        return appointmentRepository.findAllForStaffPlanning();
    }

    @Transactional
    public Appointment bookAppointment(AppointmentBookingRequestDTO dto, Long userId) {
        Order order = orderRepository.findWithDetailsById(dto.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
        if (!order.getClient().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande n'appartient pas à l'utilisateur");
        }

        UserAddress address = userAddressRepository.findByAddressIdAndUser_UserId(dto.getAddressId(), userId)
                .orElseThrow(() -> {
                    if (userAddressRepository.existsById(dto.getAddressId())) {
                        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Cette adresse n'appartient pas à l'utilisateur");
                    }
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Adresse introuvable");
                });

        TimeSlot slot = timeSlotRepository.findById(dto.getTimeSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));

        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce créneau n'est plus disponible");
        }

        SlotType expectedSlotType = SlotType.valueOf(dto.getType().name());
        if (!slot.getSlotType().equals(expectedSlotType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le type de rendez-vous ne correspond pas au créneau");
        }

        if (slot.getStartAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce créneau est déjà passé");
        }

        long booked = appointmentRepository.countBySlot_SlotIdAndStatusNot(slot.getSlotId(), AppointmentStatus.CANCELLED);
        if (booked >= slot.getCapacityMax()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plus de place disponible sur ce créneau");
        }

        Appointment appointment = new Appointment();
        appointment.setSlot(slot);
        appointment.setOrder(order);
        appointment.setAddress(address);
        appointment.setAppointmentType(dto.getType());
        appointment.setStatus(AppointmentStatus.BOOKED);

        return appointmentRepository.save(appointment);
    }

    /**
     * Employee/admin: move appointment to another time slot on a given calendar day.
     */
    @Transactional
    public Appointment rescheduleForStaff(Long appointmentId, LocalDate newDate, Long newTimeSlotId) {
        Appointment appointment = appointmentRepository.findWithStaffDetailsById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rendez-vous annulé");
        }
        if (appointment.getAppointmentType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de rendez-vous manquant");
        }

        TimeSlot oldSlot = appointment.getSlot();
        TimeSlot newSlot = timeSlotRepository.findById(newTimeSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créneau introuvable"));

        if (!Boolean.TRUE.equals(newSlot.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce créneau n'est plus disponible");
        }

        if (newSlot.getStartAt() == null || !newDate.equals(newSlot.getStartAt().toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date ne correspond pas au créneau choisi");
        }

        SlotType expectedType = SlotType.valueOf(appointment.getAppointmentType().name());
        if (!newSlot.getSlotType().equals(expectedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le type de créneau ne correspond pas au rendez-vous");
        }

        if (newSlot.getStartAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce créneau est déjà passé");
        }

        Long oldSlotId = oldSlot != null ? oldSlot.getSlotId() : null;
        if (oldSlotId == null || !newSlot.getSlotId().equals(oldSlotId)) {
            long booked = appointmentRepository.countBySlot_SlotIdAndStatusNot(newSlot.getSlotId(),
                    AppointmentStatus.CANCELLED);
            if (booked >= newSlot.getCapacityMax()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Plus de place disponible sur ce créneau");
            }
        }

        appointment.setSlot(newSlot);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(appointment);

        return appointmentRepository.findWithStaffDetailsById(appointmentId).orElse(appointment);
    }
}
