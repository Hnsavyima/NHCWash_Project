package com.nhcwash.backend.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.InvoiceDTO;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.InvoicePdfService;
import com.nhcwash.backend.services.InvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Factures", description = "Consultation des factures et PDF")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final UserRepository userRepository;
    private final DtoConverter dtoConverter;

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une facture")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable(name = "id") Long invoiceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var invoice = invoiceService.getInvoiceByIdForUser(invoiceId, user.getUserId());
                    return ResponseEntity.ok(dtoConverter.toInvoiceDto(invoice));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Same PDF bytes as {@code GET /api/orders/{orderId}/invoice} (single Thymeleaf template + {@link InvoicePdfService}).
     */
    @GetMapping("/{id}/pdf")
    @Operation(summary = "Téléchargement du PDF de facture")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable(name = "id") Long invoiceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var order = invoiceService.getOrderForInvoicePdfByInvoiceId(invoiceId, user.getUserId());
                    byte[] pdf = invoicePdfService.generatePaidOrderInvoicePdf(order);
                    String filename = "facture_CMD-" + String.format("%03d", order.getOrderId()) + ".pdf";
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
