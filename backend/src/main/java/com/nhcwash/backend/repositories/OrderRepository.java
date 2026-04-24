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

    @EntityGraph(attributePaths = {"items", "items.service", "payments", "client"})
    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByClient_UserId(Long clientId);

    @EntityGraph(attributePaths = {"items", "items.service", "payments"})
    List<Order> findByClient_UserIdOrderByCreatedAtDesc(Long clientId);

    List<Order> findByStatus(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.service", "payments", "client"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "items.service", "payments", "client", "invoice", "invoice.lines"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.client.userId = :userId")
    Optional<Order> findWithDetailsByIdAndClient_UserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    /** Client, payments and invoice lines for post-status email + optional PDF attachment. */
    @EntityGraph(attributePaths = {"client", "payments", "invoice", "invoice.lines"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findWithNotificationDetailsById(@Param("id") Long id);

    /**
     * Single round-trip fetch for transactional email rendering (items, services, client, payments).
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.client
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.service
            LEFT JOIN FETCH o.payments
            WHERE o.orderId = :id
            """)
    Optional<Order> findWithItemsAndClientForEmail(@Param("id") Long id);
}
