package com.nhcwash.backend.models.dtos.DTOConvertor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nhcwash.backend.models.dtos.AdminUserListDTO;
import com.nhcwash.backend.models.dtos.AppointmentDTO;
import com.nhcwash.backend.models.dtos.InvoiceDTO;
import com.nhcwash.backend.models.dtos.StaffPlanningAppointmentDTO;
import com.nhcwash.backend.models.dtos.InvoiceLineDTO;
import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderItemDTO;
import com.nhcwash.backend.models.dtos.PaymentDTO;
import com.nhcwash.backend.models.dtos.ServiceDTO;
import com.nhcwash.backend.models.dtos.TimeSlotDTO;
import com.nhcwash.backend.models.dtos.UserAddressDTO;
import com.nhcwash.backend.models.dtos.UserDTO;
import com.nhcwash.backend.models.entities.Appointment;
import com.nhcwash.backend.models.entities.Invoice;
import com.nhcwash.backend.models.entities.InvoiceLine;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.models.entities.Service;
import com.nhcwash.backend.models.entities.TimeSlot;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.entities.UserAddress;
import com.nhcwash.backend.util.LanguagePreference;

@Component
public class DtoConverter {

    // Convertit User vers UserDTO
    public UserDTO toUserDto(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setPreferredLanguage(LanguagePreference.normalize(user.getPreferredLanguage()));
        dto.setAvatarUrl(user.getAvatarUrl());

        Set<String> roleNames = user.getRole() != null
                ? Set.of(user.getRole().getName())
                : Set.of();
        dto.setRoles(roleNames);

        return dto;
    }

    public TimeSlotDTO toTimeSlotDto(TimeSlot slot, Integer remainingCapacity) {
        if (slot == null) return null;
        TimeSlotDTO dto = new TimeSlotDTO();
        dto.setId(slot.getSlotId());
        dto.setSlotType(slot.getSlotType());
        dto.setStartAt(slot.getStartAt());
        dto.setEndAt(slot.getEndAt());
        dto.setCapacityMax(slot.getCapacityMax());
        dto.setRemainingCapacity(remainingCapacity);
        dto.setActive(slot.getIsActive());
        return dto;
    }

    public AppointmentDTO toAppointmentDto(Appointment appointment) {
        if (appointment == null) return null;
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getAppointmentId());
        dto.setOrderId(appointment.getOrder() != null ? appointment.getOrder().getOrderId() : null);
        dto.setTimeSlotId(appointment.getSlot() != null ? appointment.getSlot().getSlotId() : null);
        dto.setAddressId(appointment.getAddress() != null ? appointment.getAddress().getAddressId() : null);
        dto.setAppointmentType(appointment.getAppointmentType());
        dto.setStatus(appointment.getStatus());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setUpdatedAt(appointment.getUpdatedAt());
        return dto;
    }

    public UserAddressDTO toUserAddressDto(UserAddress address) {
        if (address == null) return null;
        UserAddressDTO dto = new UserAddressDTO();
        dto.setId(address.getAddressId());
        dto.setLabel(address.getLabel());
        dto.setStreet(address.getStreet());
        dto.setNumber(address.getNumber());
        dto.setBox(address.getBox());
        dto.setPostalCode(address.getPostalCode());
        dto.setCity(address.getCity());
        dto.setCountry(address.getCountry());
        dto.setDefaultAddress(address.getIsDefault());
        return dto;
    }

    public ServiceDTO toServiceDto(Service s, String lang) {
        if (s == null) return null;
        ServiceDTO dto = new ServiceDTO();
        dto.setId(s.getServiceId());
        dto.setPrice(s.getBasePrice() != null ? s.getBasePrice().doubleValue() : null);
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setActive(s.getIsActive());
        dto.setCategoryId(s.getCategory() != null ? s.getCategory().getCategoryId() : null);
        dto.setEstimatedDelayHours(s.getEstimatedDelayHours());
        return dto;
    }



    // À ajouter dans votre classe DtoConverter existante

    public OrderDTO toOrderDto(Order order, String lang) {
        if (order == null) return null;

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getOrderId());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setTotalPrice(order.getFinalTotal() != null ? order.getFinalTotal().doubleValue() : null);
        dto.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        dto.setInstructions(order.getInstructions());
        dto.setCheckoutPaymentMode(order.getCheckoutPaymentMode() != null ? order.getCheckoutPaymentMode().name() : "ONLINE");
        String paymentStatus = "PENDING";
        String paymentMethod = null;
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            var refunded = order.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                    .max(Comparator.comparing(Payment::getPaymentId, Comparator.nullsFirst(Long::compareTo)));
            if (refunded.isPresent()) {
                Payment p = refunded.get();
                paymentStatus = PaymentStatus.REFUNDED.name();
                if (p.getMethod() != null) {
                    paymentMethod = p.getMethod().name();
                }
            } else {
                var succeeded = order.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                        .max(Comparator.comparing(Payment::getPaymentId, Comparator.nullsFirst(Long::compareTo)));
                if (succeeded.isPresent()) {
                    Payment p = succeeded.get();
                    paymentStatus = PaymentStatus.SUCCEEDED.name();
                    if (p.getMethod() != null) {
                        paymentMethod = p.getMethod().name();
                    }
                } else {
                    paymentStatus = order.getPayments().stream()
                            .max(Comparator.comparing(Payment::getPaymentId, Comparator.nullsFirst(Long::compareTo)))
                            .map(p -> p.getStatus().name())
                            .orElse("PENDING");
                }
            }
        }
        if (order.getRefundDate() == null
                && !PaymentStatus.REFUNDED.name().equals(paymentStatus)
                && !PaymentStatus.SUCCEEDED.name().equals(paymentStatus)
                && (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED)) {
            paymentStatus = "PAID";
        }
        dto.setPaymentStatus(paymentStatus);
        dto.setPaymentMethod(paymentMethod);
        if (order.getRefundDate() != null) {
            dto.setRefundDate(order.getRefundDate().toString());
        }
        dto.setRefundMethod(order.getRefundMethod());
        dto.setRefundReference(order.getRefundReference());

        List<OrderItemDTO> itemDtos = order.getItems().stream().map(item -> {
            OrderItemDTO itemDto = new OrderItemDTO();
            itemDto.setServiceName(item.getService() != null ? item.getService().getName() : null);
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPriceEstimated() != null
                    ? item.getUnitPriceEstimated().doubleValue() : null);
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        if (order.getClient() != null) {
            User c = order.getClient();
            dto.setClientFirstName(c.getFirstName());
            dto.setClientLastName(c.getLastName());
            dto.setClientEmail(c.getEmail());
            String fn = c.getFirstName() != null ? c.getFirstName().trim() : "";
            String ln = c.getLastName() != null ? c.getLastName().trim() : "";
            String full = (fn + " " + ln).trim();
            dto.setClientName(full.isEmpty() ? c.getEmail() : full);
        }
        return dto;
    }

    public PaymentDTO toPaymentDto(Payment payment) {
        if (payment == null) return null;
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getPaymentId());
        dto.setOrderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : null);
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setProvider(payment.getProvider());
        dto.setMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());
        dto.setProviderTxId(payment.getProviderTxId());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    public InvoiceLineDTO toInvoiceLineDto(InvoiceLine line) {
        if (line == null) return null;
        InvoiceLineDTO dto = new InvoiceLineDTO();
        dto.setId(line.getInvoiceLineId());
        dto.setLabel(line.getLabel());
        dto.setQuantity(line.getQuantity());
        dto.setUnitPrice(line.getUnitPrice());
        dto.setLineTotal(line.getLineTotal());
        return dto;
    }

    public InvoiceDTO toInvoiceDto(Invoice invoice) {
        if (invoice == null) return null;
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getInvoiceId());
        dto.setOrderId(invoice.getOrder() != null ? invoice.getOrder().getOrderId() : null);
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setIssuedAt(invoice.getIssuedAt());
        dto.setVatRate(invoice.getVatRate());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setVatAmount(invoice.getVatAmount());
        dto.setTotal(invoice.getTotal());
        dto.setPdfPath(invoice.getPdfPath());
        if (invoice.getLines() != null) {
            dto.setLines(invoice.getLines().stream().map(this::toInvoiceLineDto).collect(Collectors.toList()));
        }
        return dto;
    }

    public AdminUserListDTO toAdminUserListDto(User user) {
        if (user == null) return null;
        AdminUserListDTO dto = new AdminUserListDTO();
        dto.setId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRoles(user.getRole() != null ? Set.of(user.getRole().getName()) : Set.of());
        dto.setActive(Boolean.TRUE.equals(user.getIsActive()));
        dto.setDeleted(Boolean.TRUE.equals(user.getIsDeleted()) || user.getDeletedAt() != null);
        dto.setDeletedAt(user.getDeletedAt());
        dto.setSuspensionReason(user.getSuspensionReason());
        return dto;
    }

    public StaffPlanningAppointmentDTO toStaffPlanningAppointmentDto(Appointment a) {
        if (a == null) return null;
        StaffPlanningAppointmentDTO dto = new StaffPlanningAppointmentDTO();
        dto.setId(a.getAppointmentId());
        dto.setOrderId(a.getOrder() != null ? a.getOrder().getOrderId() : null);
        dto.setAppointmentType(a.getAppointmentType() != null ? a.getAppointmentType().name() : null);
        dto.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        if (a.getSlot() != null) {
            dto.setSlotStartAt(a.getSlot().getStartAt() != null ? a.getSlot().getStartAt().toString() : null);
            dto.setSlotEndAt(a.getSlot().getEndAt() != null ? a.getSlot().getEndAt().toString() : null);
            dto.setSlotType(a.getSlot().getSlotType() != null ? a.getSlot().getSlotType().name() : null);
        }
        if (a.getOrder() != null && a.getOrder().getClient() != null) {
            User c = a.getOrder().getClient();
            dto.setClientName((c.getFirstName() + " " + c.getLastName()).trim());
        }
        if (a.getAddress() != null) {
            var addr = a.getAddress();
            dto.setAddressSummary(
                    java.util.stream.Stream.of(addr.getStreet(), addr.getPostalCode(), addr.getCity(), addr.getCountry())
                            .filter(x -> x != null && !x.isBlank())
                            .collect(Collectors.joining(", ")));
        }
        return dto;
    }
}
