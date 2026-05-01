package com.nhcwash.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.MarkAsPaidRequestDto;
import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderRefundRequestDto;
import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.services.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Admin-style order actions also available to employees when explicitly allowed in
 * {@link com.nhcwash.backend.configs.WebSecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin — Commandes", description = "Paiement manuel et remboursement")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final String DEFAULT_LANG = "fr";

    private final OrderService orderService;
    private final DtoConverter dtoConverter;

    /**
     * Records manual payment (cash or on-site terminal), sets order to {@code PAID}, creates invoice if needed.
     * Idempotent if a succeeded payment already exists.
     */
    @PostMapping("/{id}/mark-as-paid")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    @Operation(summary = "Marquer une commande comme payée (manuel)")
    public ResponseEntity<OrderDTO> markAsPaid(@PathVariable(name = "id") Long id,
            @RequestBody(required = false) MarkAsPaidRequestDto body,
            Authentication authentication) {
        PaymentMethod method = body != null && body.getMethod() != null ? body.getMethod() : PaymentMethod.CASH;
        String actor = authentication != null ? authentication.getName() : null;
        Order order = orderService.markOrderAsPaidManually(id, method, actor);
        return ResponseEntity.ok(dtoConverter.toOrderDto(order, DEFAULT_LANG));
    }

    /**
     * Full refund — same behaviour as {@code POST /api/employee/orders/{id}/refund}; exposed under {@code /api/admin}
     * so back-office UIs can use one prefix (see WebSecurityConfig for EMPLOYEE access).
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ADMIN', 'EMPLOYEE', 'Admin', 'Employee')")
    @Operation(summary = "Rembourser une commande")
    public ResponseEntity<OrderDTO> refundOrder(@PathVariable(name = "id") Long id,
            @RequestBody(required = false) OrderRefundRequestDto body) {
        System.out.println("Refund endpoint hit by user: "
                + SecurityContextHolder.getContext().getAuthentication().getName());
        String note = body != null ? body.getManualNote() : null;
        var updated = orderService.refundOrder(id, note);
        return ResponseEntity.ok(dtoConverter.toOrderDto(updated, DEFAULT_LANG));
    }
}
