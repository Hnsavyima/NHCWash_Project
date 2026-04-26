package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @PostConstruct
    public void initStripeApiKey() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            log.warn("stripe.api.key is empty — Stripe Checkout will fail until configured");
            return;
        }
        Stripe.apiKey = stripeApiKey.trim();
    }

    /**
     * Creates a Stripe Checkout Session for the given order (EUR, line items from {@link Order#getItems()}).
     */
    public String createCheckoutSession(Order order, String frontendUrl) {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Paiement en ligne indisponible : clé Stripe non configurée");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande sans lignes");
        }

        String base = frontendUrl == null ? "" : frontendUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "URL frontend invalide");
        }

        Long orderId = order.getOrderId();
        String successUrl = base + "/dashboard/orders/" + orderId
                + "?payment=success&session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = base + "/dashboard/orders/" + orderId + "?payment=cancel";

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(String.valueOf(orderId))
                .putMetadata("order_id", String.valueOf(orderId))
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl);

        int lineItemsAdded = 0;
        for (OrderItem item : order.getItems()) {
            BigDecimal unit = item.getUnitPriceEstimated() != null ? item.getUnitPriceEstimated() : BigDecimal.ZERO;
            long unitAmountCents = eurosToStripeCents(unit);
            if (unitAmountCents <= 0) {
                continue;
            }
            int rawQty = item.getQuantity() != null ? item.getQuantity() : 0;
            long quantity = Math.max(1L, rawQty);
            String name = item.getArticleType();
            if (name == null || name.isBlank()) {
                name = item.getService() != null && item.getService().getName() != null
                        ? item.getService().getName()
                        : "Service";
            }
            paramsBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(quantity)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("eur")
                                            .setUnitAmount(unitAmountCents)
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(name)
                                                            .build())
                                            .build())
                            .build());
            lineItemsAdded++;
        }

        if (lineItemsAdded == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Montant de commande invalide pour Stripe (montants unitaires requis)");
        }

        SessionCreateParams params = paramsBuilder.build();

        try {
            Session session = Session.create(params);
            if (session.getUrl() == null || session.getUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe n'a pas retourné d'URL de session");
            }
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment gateway error");
        }
    }

    /**
     * Stripe {@code unit_amount} is in the smallest currency unit (cents for EUR), as a non-negative integer.
     * Uses {@link BigDecimal} end-to-end to avoid floating-point drift.
     */
    private static long eurosToStripeCents(BigDecimal unitPriceEur) {
        if (unitPriceEur == null || unitPriceEur.signum() <= 0) {
            return 0L;
        }
        return unitPriceEur
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * Creates a full refund for a Checkout Session ({@code cs_…}) or PaymentIntent ({@code pi_…}) id stored as
     * {@code providerTxId}. Returns the Stripe refund id ({@code re_…}).
     */
    public String refundStripePayment(String providerTxId) {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Remboursement Stripe indisponible : clé Stripe non configurée");
        }
        String id = providerTxId == null ? "" : providerTxId.trim();
        if (id.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant de transaction Stripe manquant");
        }
        try {
            if (id.startsWith("cs_")) {
                SessionRetrieveParams sp = SessionRetrieveParams.builder().addExpand("payment_intent").build();
                Session session = Session.retrieve(id, sp, null);
                String paymentIntentId = resolvePaymentIntentId(session);
                if (paymentIntentId == null || paymentIntentId.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Session Stripe sans payment intent — remboursement impossible");
                }
                Refund refund = Refund.create(
                        RefundCreateParams.builder().setPaymentIntent(paymentIntentId).build());
                return refund.getId();
            }
            if (id.startsWith("pi_")) {
                Refund refund = Refund.create(RefundCreateParams.builder().setPaymentIntent(id).build());
                return refund.getId();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Identifiant Stripe non supporté pour un remboursement (attendu cs_… ou pi_…)");
        } catch (StripeException e) {
            log.error("Stripe refund error: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe : " + e.getMessage());
        }
    }

    private static String resolvePaymentIntentId(Session session) {
        if (session == null) {
            return null;
        }
        Object expanded = session.getPaymentIntentObject();
        if (expanded instanceof PaymentIntent pi) {
            return pi.getId();
        }
        return session.getPaymentIntent();
    }
}
