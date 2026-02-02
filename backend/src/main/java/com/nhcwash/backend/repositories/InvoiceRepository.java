package com.nhcwash.backend.repositories;

import com.nhcwash.backend.models.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrder_OrderId(Long orderId);
}
