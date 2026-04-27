package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.OrderRequestDTO;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.CheckoutPaymentMode;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.enumerations.PaymentProvider;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.PaymentRepository;
import com.nhcwash.backend.repositories.ServiceRepository;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.util.OrderReferenceParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final MailService mailService;

    public Order createOrder(OrderRequestDTO dto, Long clientId) {
        User client = userRepository.findById(clientId).orElseThrow();
        CheckoutPaymentMode checkoutMode = dto.getCheckoutPaymentMode() != null
                ? dto.getCheckoutPaymentMode()
                : CheckoutPaymentMode.ONLINE;

        Order order = new Order();
        order.setClient(client);
        order.setCheckoutPaymentMode(checkoutMode);
        if (checkoutMode == CheckoutPaymentMode.CASH_ON_SITE) {
            order.setStatus(OrderStatus.PENDING);
        } else {
            order.setStatus(OrderStatus.RECEIVED);
        }
        order.setInstructions(dto.getInstructions());

        Set<OrderItem> items = dto.getItems().stream().map(i -> {
            OrderItem item = new OrderItem();

            com.nhcwash.backend.models.entities.Service s = serviceRepository.findById(i.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service non trouvé"));

            item.setService(s);
            item.setQuantity(i.getQuantity());
            item.setOrder(order);
            item.setArticleType(s.getName());
            item.setUnitPriceEstimated(s.getBasePrice());
            if (s.getBasePrice() != null) {
                item.setLineTotalEstimated(s.getBasePrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            }
            return item;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        order.setItems(items);

        BigDecimal estimatedTotal = items.stream()
                .map(OrderItem::getLineTotalEstimated)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setEstimatedTotal(estimatedTotal);
        order.setFinalTotal(null);

        Order saved = orderRepository.save(order);
        if (checkoutMode == CheckoutPaymentMode.CASH_ON_SITE) {
            Long id = saved.getOrderId();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mailService.sendPayOnSiteInstructions(id);
                    }
                });
            } else {
                mailService.sendPayOnSiteInstructions(id);
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersForClient(Long clientId) {
        return orderRepository.findByClient_UserIdOrderByCreatedAtDesc(clientId);
    }

    @Transactional(readOnly = true)
    public List<Order> findAllOrdersForStaff() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Order getOrderForStaff(Long orderId) {
        return orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
    }

    /**
     * Resolves {@code CMD-018} (or plain numeric id) then loads the order like {@link #getOrderForStaff(Long)}.
     */
    @Transactional(readOnly = true)
    public Order getOrderForStaffByReference(String reference) {
        Long id = OrderReferenceParser.tryParseOrderId(reference);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Référence de commande invalide");
        }
        return getOrderForStaff(id);
    }

    @Transactional(readOnly = true)
    public Order getOrderForClient(Long orderId, Long clientId) {
        return orderRepository.findWithDetailsByIdAndClient_UserId(orderId, clientId)
                .orElseThrow(() -> {
                    if (orderRepository.existsById(orderId)) {
                        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Cette commande n'appartient pas à l'utilisateur");
                    }
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable");
                });
    }

    /**
     * Staff workflow: advance order through operational statuses or cancel.
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        OrderStatus current = order.getStatus();
        if (current == newStatus) {
            return order;
        }

        validateStaffStatusTransition(current, newStatus);

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        scheduleOrderStatusNotification(saved.getOrderId());
        return saved;
    }

    private void scheduleOrderStatusNotification(Long orderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationService.sendOrderStatusUpdateEmail(orderId);
                }
            });
        } else {
            notificationService.sendOrderStatusUpdateEmail(orderId);
        }
    }

    /**
     * Staff may jump forward in the logistical chain (skipping intermediate steps), but not backward.
     * {@link OrderStatus#CANCELLED} is allowed from any non-terminal cancelled state.
     */
    private static void validateStaffStatusTransition(OrderStatus from, OrderStatus to) {
        if (to == OrderStatus.CANCELLED) {
            if (from == OrderStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La commande est déjà annulée.");
            }
            return;
        }
        if (from == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de modifier une commande annulée.");
        }
        List<OrderStatus> flow = Arrays.asList(
                OrderStatus.PENDING,
                OrderStatus.PAID,
                OrderStatus.RECEIVED,
                OrderStatus.PROCESSING,
                OrderStatus.READY,
                OrderStatus.DELIVERED);
        int currentIndex = flow.indexOf(from);
        int newIndex = flow.indexOf(to);
        if (currentIndex < 0 || newIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transition de statut non autorisée : " + from + " → " + to);
        }
        if (newIndex > currentIndex) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Impossible de reculer le statut de la commande.");
    }

    /**
     * Refunds the succeeded payment for an order: Stripe API for online card payments, or manual metadata for
     * on-site (cash / terminal) payments.
     */
    @Transactional
    public Order refundOrder(Long orderId, String manualNoteNullable) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
        if (order.getRefundDate() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande déjà remboursée");
        }

        Payment succeeded = paymentRepository.findFirstByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Aucun paiement réussi à rembourser"));

        LocalDateTime now = LocalDateTime.now();

        if (succeeded.getProvider() == PaymentProvider.STRIPE) {
            String txId = succeeded.getProviderTxId();
            String refundId = stripeService.refundStripePayment(txId);
            succeeded.setStatus(PaymentStatus.REFUNDED);
            order.setRefundDate(now);
            order.setRefundMethod("STRIPE_API");
            order.setRefundReference(refundId);
            paymentRepository.save(succeeded);
            Order saved = orderRepository.save(order);
            mailService.sendRefundConfirmation(saved);
            return saved;
        }

        if (succeeded.getProvider() == PaymentProvider.ONSITE) {
            String note = manualNoteNullable == null || manualNoteNullable.isBlank()
                    ? "Remboursement manuel (magasin)"
                    : manualNoteNullable.trim();
            succeeded.setStatus(PaymentStatus.REFUNDED);
            order.setRefundDate(now);
            order.setRefundMethod(succeeded.getMethod() == PaymentMethod.CASH ? "MANUAL_CASH" : "MANUAL_POS");
            order.setRefundReference(note);
            paymentRepository.save(succeeded);
            Order saved = orderRepository.save(order);
            mailService.sendRefundConfirmation(saved);
            return saved;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Remboursement non pris en charge pour cette méthode de paiement");
    }

    /**
     * Staff / admin: mark order as paid after receiving funds manually (cash or physical terminal).
     * Delegates to {@link PaymentService#recordOnsiteManualPayment}; payment method is persisted as CASH or
     * POS_TERMINAL (counter / external terminal).
     */
    @Transactional
    public Order markOrderAsPaidManually(Long orderId, PaymentMethod method, String actorEmail) {
        PaymentMethod m = method != null ? method : PaymentMethod.CASH;
        if (m != PaymentMethod.CASH && m != PaymentMethod.POS_TERMINAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Méthode non autorisée (utilisez CASH ou POS_TERMINAL)");
        }
        if (paymentRepository.existsByOrder_OrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)) {
            log.info("Order {} mark-as-paid: already has succeeded payment (actor={})", orderId, actorEmail);
        } else {
            paymentService.recordOnsiteManualPayment(orderId, m);
            log.info("Order {} marked as PAID manually (method={}, actor={})", orderId, m, actorEmail);
        }
        return orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
    }
}
