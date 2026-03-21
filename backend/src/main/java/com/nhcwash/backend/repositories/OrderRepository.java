package com.nhcwash.backend.repositories;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.enumerations.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClient_UserId(Long clientId);

    List<Order> findByStatus(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.service", "client"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findWithDetailsById(@Param("id") Long id);
}
