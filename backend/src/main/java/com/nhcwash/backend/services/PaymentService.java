package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.entities.Invoice;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.enumerations.CheckoutPaymentMode;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.enumerations.PaymentProvider;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.repositories.InvoiceRepository;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String DEFAULT_CURRENCY = "EUR";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final MailService mailService;

    public record PaymentProcessingResult(Payment payment, Invoice invoice) {
    }

    /**
     * Records an on-site payment (cash or physical card terminal) for staff. Marks order {@link OrderStatus#PAID},
     * creates invoice when missing. Idempotent if a succeeded payment already exists.
     */
    @Transactional
    public PaymentProcessingResult recordOnsiteManualPayment(Long orderId, PaymentMethod method) {
        if (method != PaymentMethod.CASH && method != PaymentMethod.POS_TERMINAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Méthode non autorisée pour un encaissement manuel (utilisez CASH ou POS_TERMINAL)");
        }

        synchronized (String.valueOf(orderId).intern()) {
            if (paymentRepository.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)) {
                Order order = orderRepository.findWithDetailsById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
                Payment payment = paymentRepository.findFirstByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Paiement enregistré introuvable"));
                Invoice invoice = invoiceService.findOrCreateInvoiceForOrder(order);
                return new PaymentProcessingResult(payment, invoice);
            }

            Order order = orderRepository.findWithDetailsById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));

            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande annulée");
            }

            if (order.getStatus() == OrderStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande déjà payée");
            }

            BigDecimal amount = resolveOrderAmount(order);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant de commande invalide");
            }

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(amount);
            payment.setCurrency(DEFAULT_CURRENCY);
            payment.setProvider(PaymentProvider.ONSITE);
            payment.setMethod(method);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setProviderTxId("ONSITE-" + orderId + "-" + System.currentTimeMillis());

            order.setStatus(OrderStatus.PAID);
            order.setFinalTotal(amount);

            paymentRepository.save(payment);
            orderRepository.save(order);

            Invoice invoice = invoiceService.findOrCreateInvoiceForOrder(order);
            schedulePaymentReceiptEmailAfterCommit(orderId);
            return new PaymentProcessingResult(payment, invoice);
        }
    }

    /**
     * After Stripe Checkout reports {@code paid}, persist payment, mark order paid, create invoice.
     * Idempotent if a succeeded payment already exists for the order.
     */
    @Transactional
    public PaymentProcessingResult confirmStripeCheckout(Long orderId, Long userId, String sessionId) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (!order.getClient().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande n'appartient pas à l'utilisateur");
        }

        if (order.getCheckoutPaymentMode() == CheckoutPaymentMode.CASH_ON_SITE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cette commande est en paiement sur place : le paiement en ligne n'est pas disponible.");
        }

        if (paymentRepository.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)) {
            Payment payment = paymentRepository.findFirstByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Paiement enregistré introuvable"));
            Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Facture introuvable pour cette commande"));
            return new PaymentProcessingResult(payment, invoice);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande annulée");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande déjà payée");
        }

        synchronized (String.valueOf(orderId).intern()) {
            if (paymentRepository.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)) {
                Payment payment = paymentRepository.findFirstByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Paiement enregistré introuvable"));
                Invoice invoice = invoiceRepository.findByOrder_OrderId(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Facture introuvable pour cette commande"));
                return new PaymentProcessingResult(payment, invoice);
            }

            Session session;
            try {
                session = Session.retrieve(sessionId.trim());
            } catch (StripeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session Stripe invalide : " + e.getMessage());
            }

            String cref = session.getClientReferenceId();
            if (cref == null || !cref.equals(String.valueOf(orderId))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Session Stripe ne correspond pas à cette commande");
            }

            if (!isStripeCheckoutPaid(session)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paiement non confirmé par Stripe");
            }

            BigDecimal expected = resolveOrderAmount(order);
            if (expected.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant de commande invalide");
            }

            long expectedCents = expected.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
            Long amountTotal = session.getAmountTotal();
            if (amountTotal == null || Math.abs(amountTotal - expectedCents) > 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant Stripe ne correspond pas à la commande");
            }

            String currency = session.getCurrency() != null ? session.getCurrency().trim().toUpperCase() : DEFAULT_CURRENCY;
            if (!"EUR".equals(currency)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Devise Stripe inattendue");
            }

            // Checkout Session id (cs_…) is stable; payment_intent may be absent until expansion in some API versions.
            String providerTxId = session.getId() != null ? session.getId() : sessionId.trim();

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(expected);
            payment.setCurrency(currency);
            payment.setProvider(PaymentProvider.STRIPE);
            payment.setMethod(PaymentMethod.CARD);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setProviderTxId(providerTxId);

            order.setStatus(OrderStatus.PAID);
            order.setFinalTotal(expected);

            paymentRepository.save(payment);
            orderRepository.save(order);

            Invoice invoice = invoiceService.createInvoiceForOrder(order);
            return new PaymentProcessingResult(payment, invoice);
        }
    }

    private static boolean isStripeCheckoutPaid(Session session) {
        try {
            Object ps = session.getPaymentStatus();
            if (ps == null) {
                return false;
            }
            return "paid".equalsIgnoreCase(ps.toString());
        } catch (Exception e) {
            return false;
        }
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

    /**
     * Receipt email loads the order in an {@code @Async} thread; scheduling after commit avoids reading stale DB
     * state (e.g. {@code final_total} still null → "0 €").
     */
    private void schedulePaymentReceiptEmailAfterCommit(Long orderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailService.sendPaymentReceipt(orderId);
                }
            });
        } else {
            mailService.sendPaymentReceipt(orderId);
        }
    }
}
