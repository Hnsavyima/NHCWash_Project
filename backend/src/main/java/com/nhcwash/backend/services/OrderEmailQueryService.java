package com.nhcwash.backend.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.repositories.OrderRepository;

@Service
public class OrderEmailQueryService {

    private final OrderRepository orderRepository;

    public OrderEmailQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Loads the order graph needed for outbound HTML mail inside a read-only transaction, so
     * {@link org.springframework.scheduling.annotation.Async} callers can traverse associations safely.
     */
    @Transactional(readOnly = true)
    public Optional<Order> loadOrderForEmail(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        return orderRepository.findWithItemsAndClientForEmail(orderId);
    }
}
