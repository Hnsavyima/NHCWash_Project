package com.nhcwash.backend.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.PaymentDTO;
import com.nhcwash.backend.repositories.PaymentRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Admin — Paiements", description = "Consultation des paiements")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final DtoConverter dtoConverter;

    @GetMapping
    @Operation(summary = "Liste des paiements")
    public ResponseEntity<List<PaymentDTO>> listPayments() {
        List<PaymentDTO> body = paymentRepository.findAllWithOrderByCreatedAtDesc().stream()
                .map(dtoConverter::toPaymentDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
