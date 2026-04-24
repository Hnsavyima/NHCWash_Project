package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.util.LanguagePreference;

@Service
public class EmailTemplateService {

    private final SpringTemplateEngine emailTemplateEngine;
    private final GlobalSettingsService globalSettingsService;

    @Value("${nhcwash.app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public EmailTemplateService(@Qualifier("emailTemplateEngine") SpringTemplateEngine emailTemplateEngine,
            GlobalSettingsService globalSettingsService) {
        this.emailTemplateEngine = emailTemplateEngine;
        this.globalSettingsService = globalSettingsService;
    }

    private String brandName() {
        try {
            String n = globalSettingsService.getSettingsEntity().getCompanyName();
            return n != null && !n.isBlank() ? n.trim() : "NHCWash";
        } catch (Exception e) {
            return "NHCWash";
        }
    }

    /** Two-decimal plain string with comma as decimal separator (EUR display). */
    public static String formatEuroAmount(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Amount shown on a receipt: succeeded payment total when present, otherwise {@link Order#getTotalAmount()}.
     */
    private static BigDecimal resolveReceiptAmount(Order order) {
        if (order.getPayments() != null) {
            BigDecimal fromPayments = order.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (fromPayments.compareTo(BigDecimal.ZERO) > 0) {
                return fromPayments;
            }
        }
        return order.getTotalAmount();
    }

    public String renderWelcome(User user) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        String base = normalizeFrontendBase(frontendBaseUrl);
        ctx.setVariable("frontendBaseUrl", base);
        ctx.setVariable("loginUrl", base + "/login");
        return emailTemplateEngine.process("welcome_" + lang, ctx);
    }

    public String welcomeSubject(User user) {
        String brand = brandName();
        return switch (LanguagePreference.normalize(user.getPreferredLanguage())) {
            case "EN" -> "Welcome to " + brand;
            case "NL" -> "Welkom bij " + brand;
            case "DE" -> "Willkommen bei " + brand;
            default -> "Bienvenue chez " + brand;
        };
    }

    public String renderPasswordReset(User user, String resetLink) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("resetLink", resetLink);
        ctx.setVariable("brandName", brandName());
        return emailTemplateEngine.process("reset_password_" + lang, ctx);
    }

    public String passwordResetSubject(User user) {
        return switch (LanguagePreference.normalize(user.getPreferredLanguage())) {
            case "EN" -> "Password reset";
            case "NL" -> "Wachtwoord opnieuw instellen";
            case "DE" -> "Passwort zurücksetzen";
            default -> "Réinitialisation du mot de passe";
        };
    }

    public String renderOrderStatusUpdate(User user, String statusMessage, Long orderId, String amountDisplay) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        ctx.setVariable("statusMessage", statusMessage);
        ctx.setVariable("orderId", orderId);
        ctx.setVariable("amountDisplay", amountDisplay != null ? amountDisplay : "—");
        String base = normalizeFrontendBase(frontendBaseUrl);
        ctx.setVariable("frontendBaseUrl", base);
        ctx.setVariable("ordersUrl", clientOrdersListUrl(frontendBaseUrl));
        return emailTemplateEngine.process("order_status_update_" + lang, ctx);
    }

    public String orderStatusUpdateSubject(User user, OrderStatus status) {
        String lang = LanguagePreference.normalize(user.getPreferredLanguage());
        String ref = status != null ? status.name() : "";
        String brand = brandName();
        return switch (lang) {
            case "EN" -> "Your " + brand + " order update (" + ref + ")";
            case "NL" -> "Update van uw " + brand + "-bestelling (" + ref + ")";
            case "DE" -> "Aktualisierung Ihrer " + brand + "-Bestellung (" + ref + ")";
            default -> "Mise à jour de votre commande " + brand + " (" + ref + ")";
        };
    }

    public static String formatOrderRef(Long orderId) {
        if (orderId == null) {
            return "CMD-000";
        }
        return "CMD-" + String.format("%03d", orderId);
    }

    public String renderPayOnSite(User user, Order order) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        String base = normalizeFrontendBase(frontendBaseUrl);
        ctx.setVariable("frontendBaseUrl", base);
        BigDecimal total = order.getTotalAmount();
        String orderRef = formatOrderRef(order.getOrderId());
        ctx.setVariable("orderRef", orderRef);
        String enc = URLEncoder.encode(orderRef, StandardCharsets.UTF_8);
        ctx.setVariable("qrImgUrl", "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + enc);
        ctx.setVariable("ordersUrl", clientOrdersListUrl(frontendBaseUrl));
        String totalStr = formatEuroAmount(total) + " €";
        ctx.setVariable("amountDisplay", totalStr);
        ctx.setVariable("totalAmount", totalStr);
        return emailTemplateEngine.process("pay_on_site_" + lang, ctx);
    }

    public String payOnSiteSubject(User user, Long orderId) {
        return switch (LanguagePreference.normalize(user.getPreferredLanguage())) {
            case "EN" -> "Order confirmation";
            case "NL" -> "Bevestiging van uw bestelling";
            case "DE" -> "Bestellbestätigung";
            default -> "Confirmation de votre commande";
        };
    }

    public String renderPaymentReceipt(User user, Order order) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        String base = normalizeFrontendBase(frontendBaseUrl);
        ctx.setVariable("frontendBaseUrl", base);
        BigDecimal paid = resolveReceiptAmount(order);
        String totalStr = formatEuroAmount(paid) + " €";
        ctx.setVariable("amountDisplay", totalStr);
        ctx.setVariable("totalAmount", totalStr);
        ctx.setVariable("orderRef", formatOrderRef(order.getOrderId()));
        ctx.setVariable("invoicesUrl", clientInvoicesListUrl(frontendBaseUrl));
        ctx.setVariable("ordersUrl", clientOrdersListUrl(frontendBaseUrl));
        ctx.setVariable("orderDetailUrl", clientOrderDetailUrl(frontendBaseUrl, order.getOrderId()));
        return emailTemplateEngine.process("payment_receipt_" + lang, ctx);
    }

    public String paymentReceiptSubject(User user, Long orderId) {
        return switch (LanguagePreference.normalize(user.getPreferredLanguage())) {
            case "EN" -> "Payment received — Thank you for your trust";
            case "NL" -> "Betaling ontvangen — Bedankt voor uw vertrouwen";
            case "DE" -> "Zahlung erhalten — Vielen Dank für Ihr Vertrauen";
            default -> "Paiement reçu - Merci de votre confiance";
        };
    }

    /** Normalized {@code nhcwash.app.frontend-base-url} (no trailing slash). */
    public static String normalizeFrontendBase(String frontendBaseUrl) {
        if (frontendBaseUrl == null) {
            return "";
        }
        return frontendBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Customer SPA — orders list. Never use {@code /backoffice}, {@code /admin}, or staff paths in client emails.
     */
    public static String clientOrdersListUrl(String frontendBaseUrl) {
        return normalizeFrontendBase(frontendBaseUrl) + "/dashboard/orders";
    }

    /**
     * Customer SPA — single order detail ({@code OrderDetailPage}).
     */
    public static String clientOrderDetailUrl(String frontendBaseUrl, Long orderId) {
        if (orderId == null) {
            return clientOrdersListUrl(frontendBaseUrl);
        }
        return normalizeFrontendBase(frontendBaseUrl) + "/dashboard/orders/" + orderId;
    }

    /** Customer SPA — invoices list ({@code ClientInvoices}). */
    public static String clientInvoicesListUrl(String frontendBaseUrl) {
        return normalizeFrontendBase(frontendBaseUrl) + "/dashboard/invoices";
    }

    public String renderAccountDeleted(User user, boolean deletedByAdmin) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        ctx.setVariable("deletedByAdmin", deletedByAdmin);
        String base = normalizeFrontendBase(frontendBaseUrl);
        ctx.setVariable("contactUrl", base + "/contact");
        return emailTemplateEngine.process("account_deleted_" + lang, ctx);
    }

    public String accountDeletedSubject(User user, boolean deletedByAdmin) {
        String lang = LanguagePreference.normalize(user.getPreferredLanguage());
        if (deletedByAdmin) {
            return switch (lang) {
                case "EN" -> "Your account has been archived by an administrator";
                case "NL" -> "Uw account is door een beheerder gearchiveerd";
                case "DE" -> "Ihr Konto wurde von einem Administrator archiviert";
                default -> "Suppression de votre compte NHCWash";
            };
        }
        return switch (lang) {
            case "EN" -> "Confirmation of account deletion";
            case "NL" -> "Bevestiging van verwijdering van uw account";
            case "DE" -> "Bestätigung der Kontolöschung";
            default -> "Suppression de votre compte NHCWash";
        };
    }

    public String renderAccountRestored(User user) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        String base = normalizeFrontendBase(frontendBaseUrl);
        ctx.setVariable("loginUrl", base + "/login");
        return emailTemplateEngine.process("account_restored_" + lang, ctx);
    }

    public String accountRestoredSubject(User user) {
        return switch (LanguagePreference.normalize(user.getPreferredLanguage())) {
            case "EN" -> "Your account has been reactivated";
            case "NL" -> "Uw account is opnieuw geactiveerd";
            case "DE" -> "Ihr Konto wurde wieder aktiviert";
            default -> "Votre compte a été réactivé";
        };
    }

    public String renderRefundConfirmation(User user, Order order, BigDecimal refundAmount) {
        String lang = LanguagePreference.templateSuffix(user.getPreferredLanguage());
        Context ctx = new Context();
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("brandName", brandName());
        ctx.setVariable("orderRef", formatOrderRef(order.getOrderId()));
        BigDecimal amt = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        ctx.setVariable("refundAmount", formatEuroAmount(amt) + " €");
        return emailTemplateEngine.process("refund_confirmation_" + lang, ctx);
    }

    public String refundConfirmationSubject(User user) {
        return switch (LanguagePreference.normalize(user.getPreferredLanguage())) {
            case "EN" -> "Order refund confirmation";
            case "NL" -> "Bevestiging van terugbetaling van uw bestelling";
            case "DE" -> "Bestätigung der Rückerstattung Ihrer Bestellung";
            default -> "Remboursement de votre commande";
        };
    }
}
