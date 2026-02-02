package com.nhcwash.backend.repositories;


import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClient_UserId(Long clientId);

    List<Order> findByStatus(OrderStatus status);
}
