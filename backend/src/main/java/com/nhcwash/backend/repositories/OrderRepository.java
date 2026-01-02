package com.nhcwash.backend.repositories;


import com.nhcwash.backend.models.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientId(Long clientId);

    List<Order> findByStatus(String status);
}
