package com.nhcwash.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.enumerations.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder_OrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findFirstByOrder_OrderIdAndStatus(Long orderId, PaymentStatus status);

    @EntityGraph(attributePaths = { "order" })
    @Query("SELECT p FROM Payment p ORDER BY p.createdAt DESC")
    List<Payment> findAllWithOrderByCreatedAtDesc();
}
