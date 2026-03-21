package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.OrderRequestDTO;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.ServiceRepository;
import com.nhcwash.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public Order createOrder(OrderRequestDTO dto, Long clientId) {
        User client = userRepository.findById(clientId).orElseThrow();
        Order order = new Order();
        order.setClient(client);
        order.setStatus(OrderStatus.RECEIVED);
        order.setInstructions(dto.getInstructions());

        List<OrderItem> items = dto.getItems().stream().map(i -> {
            OrderItem item = new OrderItem();

            com.nhcwash.backend.models.entities.Service s = serviceRepository.findById(i.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));

            item.setService(s);
            item.setQuantity(i.getQuantity());
            item.setOrder(order);
            item.setArticleType(s.getName());
            item.setUnitPriceEstimated(s.getBasePrice());
            if (s.getBasePrice() != null) {
                item.setLineTotalEstimated(s.getBasePrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            }
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);

        BigDecimal estimatedTotal = items.stream()
                .map(OrderItem::getLineTotalEstimated)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setEstimatedTotal(estimatedTotal);
        order.setFinalTotal(null);

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersForClient(Long clientId) {
        return orderRepository.findByClient_UserIdOrderByCreatedAtDesc(clientId);
    }

    @Transactional(readOnly = true)
    public Order getOrderForClient(Long orderId, Long clientId) {
        return orderRepository.findWithDetailsByIdAndClient_UserId(orderId, clientId)
                .orElseThrow(() -> {
                    if (orderRepository.existsById(orderId)) {
                        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Cette commande n'appartient pas à l'utilisateur");
                    }
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable");
                });
    }
}
