package com.nhcwash.backend.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nhcwash.backend.models.constants.RoleNames;
import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderRequestDTO;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.InvoicePdfService;
import com.nhcwash.backend.services.OrderService;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Commandes (client)", description = "Liste, détail, facture PDF et création de commande")
@RequiredArgsConstructor
public class OrderController {

    private static final String DEFAULT_LANG = "fr";

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final DtoConverter dtoConverter;
    private final InvoicePdfService invoicePdfService;

    @GetMapping
    @Operation(summary = "Liste des commandes du client connecté")
    public ResponseEntity<List<OrderDTO>> listMyOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    List<OrderDTO> body = orderService.findOrdersForClient(user.getUserId()).stream()
                            .map(o -> dtoConverter.toOrderDto(o, DEFAULT_LANG))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commande")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable(name = "id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var order = orderService.getOrderForClient(id, user.getUserId());
                    return ResponseEntity.ok(dtoConverter.toOrderDto(order, DEFAULT_LANG));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * PDF invoice. Clients may only download their own order ({@link OrderService#getOrderForClient});
     * staff may download any order ({@link OrderService#getOrderForStaff}). No {@code @PreAuthorize} — URL
     * rules are in {@link com.nhcwash.backend.configs.WebSecurityConfig}.
     */
    @GetMapping("/{id}/invoice")
    @Operation(summary = "Téléchargement de la facture PDF")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable(name = "id") Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var order = isStaffOrderInvoiceAccess(authentication)
                            ? orderService.getOrderForStaff(id)
                            : orderService.getOrderForClient(id, user.getUserId());
                    byte[] pdf = invoicePdfService.generatePaidOrderInvoicePdf(order);
                    String filename = "facture_CMD-" + String.format("%03d", id) + ".pdf";
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /** Admin / employee: full order access for support and back-office (not limited to own client id). */
    private static boolean isStaffOrderInvoiceAccess(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> RoleNames.ADMIN.equals(a) || RoleNames.EMPLOYEE.equals(a)
                        || "ADMIN".equals(a) || "EMPLOYEE".equals(a)
                        || "ROLE_Admin".equals(a) || "Admin".equals(a) || "Employee".equals(a));
    }

    @PostMapping
    @Operation(summary = "Passer une commande")
    public ResponseEntity<OrderDTO> placeOrder(@Valid @RequestBody OrderRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var order = orderService.createOrder(dto, user.getUserId());
                    return ResponseEntity.ok(dtoConverter.toOrderDto(order, DEFAULT_LANG));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
