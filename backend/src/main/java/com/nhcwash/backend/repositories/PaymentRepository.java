package com.nhcwash.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
