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
        ServiceDTO dto = new ServiceDTO();
        dto.setId(s.getId());
        dto.setPrice(s.getBasePrice());
        dto.setName("en".equals(lang) ? s.getNameEn() : s.getNameFr());
        dto.setDescription("en".equals(lang) ? s.getDescriptionEn() : s.getDescriptionFr());
        return dto;
    }



    // À ajouter dans votre classe DtoConverter existante

    public OrderDTO toOrderDto(Order order, String lang) {
        if (order == null) return null;

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setCreatedAt(order.getCreatedAt().toString());
        dto.setPaymentStatus(order.getPayment() != null ? order.getPayment().getStatus() : "UNPAID");

        List<OrderItemDTO> itemDtos = order.getItems().stream().map(item -> {
            OrderItemDTO itemDto = new OrderItemDTO();
            // Utilisation de la logique de langue pour le nom du service
            itemDto.setServiceName("en".equals(lang) ? 
                item.getService().getNameEn() : item.getService().getNameFr());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getService().getBasePrice());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        return dto;
    }
}
