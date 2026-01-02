package com.nhcwash.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.OrderRequestDTO;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.services.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequestDTO dto) {
        // En prod, récupérez l'ID via le SecurityContext (JWT)
        return ResponseEntity.ok(orderService.createOrder(dto, 1L));
    }
}
