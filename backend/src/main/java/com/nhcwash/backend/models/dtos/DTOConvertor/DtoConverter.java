package com.nhcwash.backend.models.dtos.DTOConvertor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderItemDTO;
import com.nhcwash.backend.models.dtos.ServiceDTO;
import com.nhcwash.backend.models.dtos.UserDTO;
import com.nhcwash.backend.models.entities.Order;
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
        dto.setPaymentStatus(order.getPayment() != null ? order.getPayment().getStatus() : "UNPAID");

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
}
