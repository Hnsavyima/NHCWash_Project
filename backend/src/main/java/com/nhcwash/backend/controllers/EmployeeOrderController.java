package com.nhcwash.backend.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.ManualPaymentRequest;
import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderRefundRequestDto;
import com.nhcwash.backend.models.dtos.OrderStatusUpdateDTO;
import com.nhcwash.backend.services.OrderService;
import com.nhcwash.backend.services.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Staff-only listing of all client orders (back-office dashboards).
 * Secured via {@code /api/employee/**} in WebSecurityConfig.
 */
@RestController
@RequestMapping("/api/employee/orders")
@Tag(name = "Employé — Commandes", description = "Back-office des commandes")
@RequiredArgsConstructor
public class EmployeeOrderController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeOrderController.class);

    private static final String DEFAULT_LANG = "fr";

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DtoConverter dtoConverter;

    @GetMapping
    @Operation(summary = "Liste de toutes les commandes (staff)")
    public ResponseEntity<List<OrderDTO>> listAllOrders() {
        List<OrderDTO> body = orderService.findAllOrdersForStaff().stream()
                .map(o -> dtoConverter.toOrderDto(o, DEFAULT_LANG))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    /**
     * Lookup by public reference (e.g. {@code CMD-018} from customer QR) or numeric id string.
     */
    @GetMapping("/by-reference/{ref}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    @Operation(summary = "Rechercher une commande par référence")
    public ResponseEntity<OrderDTO> getOrderForStaffByReference(@PathVariable(name = "ref") String ref) {
        var order = orderService.getOrderForStaffByReference(ref);
        return ResponseEntity.ok(dtoConverter.toOrderDto(order, DEFAULT_LANG));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    @Operation(summary = "Détail commande (staff)")
    public ResponseEntity<OrderDTO> getOrderForStaff(@PathVariable(name = "id") Long id) {
        var order = orderService.getOrderForStaff(id);
        return ResponseEntity.ok(dtoConverter.toOrderDto(order, DEFAULT_LANG));
    }

    /**
     * Counter payment (cash or physical terminal) — bypasses Stripe.
     */
    @PostMapping("/{id}/pay-manual")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    @Operation(summary = "Enregistrer un paiement sur place")
    public ResponseEntity<OrderDTO> payManual(@PathVariable(name = "id") Long id,
            @Valid @RequestBody ManualPaymentRequest body) {
        paymentService.recordOnsiteManualPayment(id, body.getMethod());
        var updated = orderService.getOrderForStaff(id);
        return ResponseEntity.ok(dtoConverter.toOrderDto(updated, DEFAULT_LANG));
    }

    /**
     * Advance or cancel order status (employee / admin). URL prefix is also restricted in
     * {@link com.nhcwash.backend.configs.WebSecurityConfig}.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_EMPLOYEE', 'EMPLOYEE')")
    @Operation(summary = "Mettre à jour le statut d'une commande")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable(name = "id") Long id,
            @Valid @RequestBody OrderStatusUpdateDTO body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug(
                "PATCH /api/employee/orders/{}/status status={} principal={} authorities={}",
                id,
                body.getStatus(),
                auth != null ? auth.getName() : "null",
                auth != null ? auth.getAuthorities() : "null");
        var updated = orderService.updateOrderStatus(id, body.getStatus());
        return ResponseEntity.ok(dtoConverter.toOrderDto(updated, DEFAULT_LANG));
    }

    /**
     * Full refund of the succeeded payment (Stripe API or manual note for on-site payments).
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    @Operation(summary = "Rembourser une commande (staff)")
    public ResponseEntity<OrderDTO> refundOrder(@PathVariable(name = "id") Long id,
            @RequestBody(required = false) OrderRefundRequestDto body) {
        String note = body != null ? body.getManualNote() : null;
        var updated = orderService.refundOrder(id, note);
        return ResponseEntity.ok(dtoConverter.toOrderDto(updated, DEFAULT_LANG));
    }
}
