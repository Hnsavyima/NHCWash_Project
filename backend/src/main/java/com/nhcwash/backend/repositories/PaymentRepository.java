package com.nhcwash.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.enumerations.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder_OrderIdAndStatus(Long orderId, PaymentStatus status);
}
