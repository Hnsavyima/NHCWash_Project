package com.nhcwash.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.PaymentRequestDTO;
import com.nhcwash.backend.models.dtos.PaymentResultDTO;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final DtoConverter dtoConverter;

    @PostMapping
    public ResponseEntity<PaymentResultDTO> pay(@Valid @RequestBody PaymentRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var result = paymentService.processPayment(dto, user.getUserId());
                    PaymentResultDTO body = new PaymentResultDTO(
                            dtoConverter.toPaymentDto(result.payment()),
                            dtoConverter.toInvoiceDto(result.invoice()));
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
