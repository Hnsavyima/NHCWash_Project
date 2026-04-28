package com.nhcwash.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nhcwash.backend.models.entities.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrder_OrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.client", "order.payments", "lines"})
    @Query("SELECT i FROM Invoice i WHERE i.invoiceId = :id")
    Optional<Invoice> findWithDetailsById(@Param("id") Long id);
}
