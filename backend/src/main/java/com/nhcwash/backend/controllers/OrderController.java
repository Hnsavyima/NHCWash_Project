package com.nhcwash.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.OrderDTO;
import com.nhcwash.backend.models.dtos.OrderRequestDTO;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.OrderService;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String DEFAULT_LANG = "fr";

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final DtoConverter dtoConverter;

    @GetMapping
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
    public ResponseEntity<OrderDTO> getOrder(@PathVariable("id") Long id) {
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

    @PostMapping
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
