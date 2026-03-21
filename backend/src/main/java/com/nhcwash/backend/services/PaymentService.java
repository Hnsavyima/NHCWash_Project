package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.PaymentRequestDTO;
import com.nhcwash.backend.models.entities.Invoice;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String DEFAULT_CURRENCY = "EUR";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    public record PaymentProcessingResult(Payment payment, Invoice invoice) {
    }

    @Transactional
    public PaymentProcessingResult processPayment(PaymentRequestDTO dto, Long userId) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (!order.getClient().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande n'appartient pas à l'utilisateur");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande annulée");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande déjà payée");
        }

        if (paymentRepository.existsByOrder_OrderIdAndStatus(order.getOrderId(), PaymentStatus.SUCCEEDED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un paiement réussi existe déjà pour cette commande");
        }

        BigDecimal amount = resolveOrderAmount(order);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant de commande invalide");
        }

        String currency = dto.getCurrency() != null && !dto.getCurrency().isBlank()
                ? dto.getCurrency().trim().toUpperCase()
                : DEFAULT_CURRENCY;

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setProvider(dto.getProvider());
        payment.setMethod(dto.getMethod());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(LocalDateTime.now());

        order.setStatus(OrderStatus.PAID);
        order.setFinalTotal(amount);

        paymentRepository.save(payment);
        orderRepository.save(order);

        Invoice invoice = invoiceService.createInvoiceForOrder(order);

        return new PaymentProcessingResult(payment, invoice);
    }

    private static BigDecimal resolveOrderAmount(Order order) {
        if (order.getFinalTotal() != null) {
            return order.getFinalTotal();
        }
        if (order.getEstimatedTotal() != null) {
            return order.getEstimatedTotal();
        }
        return order.getItems().stream()
                .map(OrderItem::getLineTotalEstimated)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
