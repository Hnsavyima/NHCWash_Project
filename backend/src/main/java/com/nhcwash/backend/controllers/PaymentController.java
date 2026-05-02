package com.nhcwash.backend.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.CheckoutSessionUrlDto;
import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.PaymentResultDTO;
import com.nhcwash.backend.models.dtos.StripePaymentConfirmDto;
import com.nhcwash.backend.models.enumerations.CheckoutPaymentMode;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.PaymentRepository;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.services.PaymentService;
import com.nhcwash.backend.services.StripeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Paiements", description = "Stripe Checkout et confirmation de paiement")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DtoConverter dtoConverter;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String appFrontendUrl;

    /**
     * Starts Stripe Checkout for the caller's order; client redirects to the returned URL.
     */
    @PostMapping("/order/{orderId}")
    @Operation(summary = "Création d'une session Stripe Checkout")
    public ResponseEntity<CheckoutSessionUrlDto> createCheckoutSession(@PathVariable(name = "orderId") Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var order = orderRepository.findWithDetailsById(orderId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
                    if (!order.getClient().getUserId().equals(user.getUserId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à cette commande");
                    }
                    if (order.getStatus() == OrderStatus.CANCELLED) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande annulée");
                    }
                    if (order.getCheckoutPaymentMode() == CheckoutPaymentMode.CASH_ON_SITE) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cette commande est en paiement sur place : aucune session Stripe n'est nécessaire.");
                    }
                    if (order.getStatus() == OrderStatus.PAID
                            || paymentRepository.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande déjà payée");
                    }
                    String url = stripeService.createCheckoutSession(order, appFrontendUrl);
                    return ResponseEntity.ok(new CheckoutSessionUrlDto(url));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    /**
     * Confirms Stripe Checkout after redirect: verifies session is paid, then records payment + invoice.
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirmation du paiement après retour Stripe")
    public ResponseEntity<PaymentResultDTO> confirmStripe(@Valid @RequestBody StripePaymentConfirmDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> {
                    var result = paymentService.confirmStripeCheckout(dto.getOrderId(), user.getUserId(),
                            dto.getSessionId());
                    PaymentResultDTO body = new PaymentResultDTO(
                            dtoConverter.toPaymentDto(result.payment()),
                            dtoConverter.toInvoiceDto(result.invoice()));
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
