package com.nhcwash.backend.models.dtos.DTOConvertor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nhcwash.backend.models.dtos.InvoiceDTO;
import com.nhcwash.backend.models.dtos.InvoiceLineDTO;
import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderItemDTO;
import com.nhcwash.backend.models.dtos.PaymentDTO;
import com.nhcwash.backend.models.dtos.ServiceDTO;
import com.nhcwash.backend.models.dtos.UserDTO;
import com.nhcwash.backend.models.entities.Invoice;
import com.nhcwash.backend.models.entities.InvoiceLine;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.entities.Service;
import com.nhcwash.backend.models.entities.User;

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

        Set<String> roleNames = user.getRole() != null
                ? Set.of(user.getRole().getName())
                : Set.of();
        dto.setRoles(roleNames);

        return dto;
    }


    public ServiceDTO toServiceDto(Service s, String lang) {
        if (s == null) return null;
        ServiceDTO dto = new ServiceDTO();
        dto.setId(s.getServiceId());
        dto.setPrice(s.getBasePrice() != null ? s.getBasePrice().doubleValue() : null);
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
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
        String paymentStatus = "PENDING";
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            paymentStatus = order.getPayments().get(order.getPayments().size() - 1).getStatus().name();
        }
        dto.setPaymentStatus(paymentStatus);

        List<OrderItemDTO> itemDtos = order.getItems().stream().map(item -> {
            OrderItemDTO itemDto = new OrderItemDTO();
            itemDto.setServiceName(item.getService() != null ? item.getService().getName() : null);
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPriceEstimated() != null
                    ? item.getUnitPriceEstimated().doubleValue() : null);
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
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
}
