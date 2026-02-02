package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // C'est l'annotation Spring

import com.nhcwash.backend.models.dtos.OrderRequestDTO;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.ServiceRepository;
import com.nhcwash.backend.repositories.UserRepository;
// ATTENTION : Ne pas importer l'entité Service ici si vous utilisez l'annotation @Service, 
// ou alors utilisez le nom complet ci-dessous.

@Service // Annotation Spring
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private UserRepository userRepository;

    public Order createOrder(OrderRequestDTO dto, Long clientId) {
        User client = userRepository.findById(clientId).orElseThrow();
        Order order = new Order();
        order.setClient(client);
        order.setStatus(OrderStatus.RECEIVED);

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
}
