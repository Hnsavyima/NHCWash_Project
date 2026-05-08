package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.nhcwash.backend.models.dtos.ContactRequest;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.util.LanguagePreference;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final MailDispatchService mailDispatchService;
    private final EmailTemplateService emailTemplateService;
    private final OrderEmailQueryService orderEmailQueryService;
    private final GlobalSettingsService globalSettingsService;

    public MailService(MailDispatchService mailDispatchService, EmailTemplateService emailTemplateService,
            OrderEmailQueryService orderEmailQueryService, GlobalSettingsService globalSettingsService) {
        this.mailDispatchService = mailDispatchService;
        this.emailTemplateService = emailTemplateService;
        this.orderEmailQueryService = orderEmailQueryService;
        this.globalSettingsService = globalSettingsService;
    }

    /**
     * Shared master layout (inline CSS unchanged). Used by {@link #wrapInMasterTemplate} and
     * {@link #generateEmailWrapper}.
     */
    private static String wrapInMasterTemplateCore(String title, String content) {
        return "<div style=\"background-color: #f4f5f7; padding: 20px; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;\">\n"
                + "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e5e7eb;\">\n"
                + "    <div style=\"background-color: #0284c7; padding: 24px 32px; text-align: left;\">\n"
                + "      <h1 style=\"color: #ffffff; margin: 0; font-size: 24px; font-weight: bold;\">NHCWash</h1>\n"
                + "      <p style=\"color: #bae6fd; margin: 4px 0 0 0; font-size: 14px;\">Laundry & dry cleaning</p>\n"
                + "    </div>\n"
                + "    <div style=\"padding: 32px; color: #374151; font-size: 16px; line-height: 1.5;\">\n"
                + "      <h2 style=\"color: #111827; margin-top: 0; margin-bottom: 20px; font-size: 20px;\">"
                + escapeHtml(title)
                + "</h2>\n"
                + "      " + content + "\n"
                + "    </div>\n"
                + "    <div style=\"background-color: #f9fafb; border-top: 1px solid #e5e7eb; padding: 16px 32px; text-align: left; color: #6b7280; font-size: 12px;\">\n"
                + "      <strong>NHCWash</strong> &middot; Professional care for your clothes\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</div>";
    }

    private String wrapInMasterTemplate(String title, String content) {
        return wrapInMasterTemplateCore(title, content);
    }

    private String generateButton(String text, String url) {
        String safeHref = escapeHtml(url != null ? url : "");
        return "<div style=\"margin: 24px 0;\"><a href=\"" + safeHref
                + "\" style=\"display: inline-block; background-color: #0284c7; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;\">"
                + text + "</a></div>";
    }

    private static String toHtmlDocument(String innerFragment) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/></head><body style=\"margin:0;padding:0;\">"
                + innerFragment + "</body></html>";
    }

    /**
     * Wraps Thymeleaf fragments (with their own {@code h1}) in the master template: title is taken from the first
     * {@code h1} plain text, body is the fragment with that {@code h1} removed. Used by order status notifications.
     */
    public static String generateEmailWrapper(String content) {
        String c = content != null ? content : "";
        String title = extractFirstH1PlainText(c);
        if (title.isBlank()) {
            title = "NHCWash";
        }
        String inner = stripFirstH1(c);
        return toHtmlDocument(wrapInMasterTemplateCore(title, inner));
    }

    private static String stripFirstH1(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        int h1 = html.indexOf("<h1");
        if (h1 < 0) {
            return html;
        }
        int h1End = html.indexOf("</h1>", h1);
        if (h1End < 0) {
            return html;
        }
        return html.substring(0, h1) + html.substring(h1End + 5);
    }

    private static String extractFirstH1PlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        int h1 = html.indexOf("<h1");
        if (h1 < 0) {
            return "";
        }
        int gt = html.indexOf('>', h1);
        int end = html.indexOf("</h1>", gt);
        if (gt < 0 || end < 0) {
            return "";
        }
        String inside = html.substring(gt + 1, end);
        return inside.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

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

    private String buildPasswordResetInnerAndTitle(User user, String resetUrl, String[] titleOut) {
        String lang = LanguagePreference.normalize(user.getPreferredLanguage());
        String name = escapeHtml(user.getFirstName() != null ? user.getFirstName().trim() : "");
        String inner;
        switch (lang) {
            case "EN" -> {
                titleOut[0] = "Password reset";
                inner = "<p>Hello " + name + ",</p>"
                        + "<p>You asked to reset your password. Click the button below to choose a new one. This link expires in <strong>24 hours</strong>.</p>"
                        + generateButton("Set a new password", resetUrl)
                        + "<p style=\"font-size: 12px; color: #6b7280;\">If the button does not work, copy this link into your browser:<br><a href=\""
                        + escapeHtml(resetUrl) + "\" style=\"color: #0284c7; word-break: break-all;\">"
                        + escapeHtml(resetUrl) + "</a></p>"
                        + "<p style=\"margin:18px 0 0;font-size:14px;color:#6b7280;\">If you did not request this, you can safely ignore this email.</p>";
            }
            case "NL" -> {
                titleOut[0] = "Wachtwoord opnieuw instellen";
                inner = "<p>Hallo " + name + ",</p>"
                        + "<p>U heeft gevraagd uw wachtwoord opnieuw in te stellen. Klik op de onderstaande knop om een nieuw wachtwoord te kiezen. Deze link verloopt binnen <strong>24 uur</strong>.</p>"
                        + generateButton("Nieuw wachtwoord instellen", resetUrl)
                        + "<p style=\"font-size: 12px; color: #6b7280;\">Als de knop niet werkt, kopieer dan deze link in uw browser:<br><a href=\""
                        + escapeHtml(resetUrl) + "\" style=\"color: #0284c7; word-break: break-all;\">"
                        + escapeHtml(resetUrl) + "</a></p>"
                        + "<p style=\"margin:18px 0 0;font-size:14px;color:#6b7280;\">Als u dit niet heeft aangevraagd, kunt u deze e-mail veilig negeren.</p>";
            }
            case "DE" -> {
                titleOut[0] = "Passwort zurücksetzen";
                inner = "<p>Hallo " + name + ",</p>"
                        + "<p>Sie haben angefordert, Ihr Passwort zurückzusetzen. Klicken Sie auf die Schaltfläche unten, um ein neues zu wählen. Dieser Link ist <strong>24 Stunden</strong> gültig.</p>"
                        + generateButton("Neues Passwort festlegen", resetUrl)
                        + "<p style=\"font-size: 12px; color: #6b7280;\">Wenn die Schaltfläche nicht funktioniert, kopieren Sie diesen Link in Ihren Browser:<br><a href=\""
                        + escapeHtml(resetUrl) + "\" style=\"color: #0284c7; word-break: break-all;\">"
                        + escapeHtml(resetUrl) + "</a></p>"
                        + "<p style=\"margin:18px 0 0;font-size:14px;color:#6b7280;\">Wenn Sie diese Anfrage nicht gestellt haben, können Sie diese E-Mail ignorieren.</p>";
            }
            default -> {
                titleOut[0] = "Réinitialisation du mot de passe";
                inner = "<p>Bonjour " + name + ",</p>"
                        + "<p>Vous avez demandé à réinitialiser votre mot de passe. Cliquez sur le bouton ci-dessous pour en choisir un nouveau. Ce lien expire dans <strong>24 heures</strong>.</p>"
                        + generateButton("Réinitialiser mon mot de passe", resetUrl)
                        + "<p style=\"font-size: 12px; color: #6b7280;\">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br><a href=\""
                        + escapeHtml(resetUrl) + "\" style=\"color: #0284c7; word-break: break-all;\">"
                        + escapeHtml(resetUrl) + "</a></p>"
                        + "<p style=\"margin:18px 0 0;font-size:14px;color:#6b7280;\">Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail en toute sécurité.</p>";
            }
        }
        return inner;
    }

    private String buildPaymentReceiptInnerAndTitle(User user, Order order, String[] titleOut) {
        String lang = LanguagePreference.normalize(user.getPreferredLanguage());
        String clientDetailUrl = EmailTemplateService.clientOrderDetailUrl(frontendBaseUrl, order.getOrderId());
        String clientInvoicesUrl = EmailTemplateService.clientInvoicesListUrl(frontendBaseUrl);
        String orderRef = escapeHtml(EmailTemplateService.formatOrderRef(order.getOrderId()));
        BigDecimal paid = resolveReceiptAmount(order);
        String totalStr = escapeHtml(EmailTemplateService.formatEuroAmount(paid) + " €");
        String name = escapeHtml(user.getFirstName() != null ? user.getFirstName().trim() : "");

        String inner;
        switch (lang) {
            case "EN" -> {
                titleOut[0] = "Payment received";
                inner = "<p>Hello " + name + ",</p>"
                        + "<p>We confirm receipt of your payment of <strong>" + totalStr + "</strong> for order <strong>"
                        + orderRef + "</strong>.</p>"
                        + generateButton("Order details", clientDetailUrl)
                        + generateButton("Download invoice", clientInvoicesUrl);
            }
            case "NL" -> {
                titleOut[0] = "Betaling ontvangen";
                inner = "<p>Hallo " + name + ",</p>"
                        + "<p>We bevestigen de ontvangst van uw betaling van <strong>" + totalStr
                        + "</strong> voor bestelling <strong>" + orderRef + "</strong>.</p>"
                        + generateButton("Bestelgegevens", clientDetailUrl)
                        + generateButton("Factuur downloaden", clientInvoicesUrl);
            }
            case "DE" -> {
                titleOut[0] = "Zahlung erhalten";
                inner = "<p>Hallo " + name + ",</p>"
                        + "<p>Wir bestätigen den Eingang Ihrer Zahlung von <strong>" + totalStr
                        + "</strong> für die Bestellung <strong>" + orderRef + "</strong>.</p>"
                        + generateButton("Bestelldetails", clientDetailUrl)
                        + generateButton("Rechnung herunterladen", clientInvoicesUrl);
            }
            default -> {
                titleOut[0] = "Paiement reçu";
                inner = "<p>Bonjour " + name + ",</p>"
                        + "<p>Nous confirmons la réception de votre paiement de <strong>" + totalStr
                        + "</strong> pour la commande <strong>" + orderRef + "</strong>.</p>"
                        + generateButton("Détail de la commande", clientDetailUrl)
                        + generateButton("Télécharger la facture", clientInvoicesUrl);
            }
        }
        return inner;
    }

    private String buildPayOnSiteInnerAndTitle(User user, Order order, String[] titleOut) {
        String lang = LanguagePreference.normalize(user.getPreferredLanguage());
        String clientOrdersUrl = EmailTemplateService.clientOrdersListUrl(frontendBaseUrl);
        String orderRef = escapeHtml(EmailTemplateService.formatOrderRef(order.getOrderId()));
        BigDecimal total = order.getTotalAmount();
        String totalStr = escapeHtml(EmailTemplateService.formatEuroAmount(total) + " €");
        String name = escapeHtml(user.getFirstName() != null ? user.getFirstName().trim() : "");
        String enc = URLEncoder.encode(EmailTemplateService.formatOrderRef(order.getOrderId()), StandardCharsets.UTF_8);
        String qrImgUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + enc;

        String inner;
        switch (lang) {
            case "EN" -> {
                titleOut[0] = "Order confirmation";
                inner = "<p>Hello " + name + ",</p>"
                        + "<p>Your order <strong>" + orderRef + "</strong> is ready. Please have <strong>" + totalStr
                        + "</strong> ready to pay on site.</p>"
                        + "<p style=\"margin:0 0 28px;text-align:center;\"><span style=\"display:inline-block;padding:14px;background:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;\">"
                        + "<img src=\"" + qrImgUrl
                        + "\" width=\"220\" height=\"220\" alt=\"Order QR code\" style=\"display:block;border-radius:6px;margin:0 auto;\"/></span></p>"
                        + generateButton("Track my order", clientOrdersUrl);
            }
            case "NL" -> {
                titleOut[0] = "Bevestiging van uw bestelling";
                inner = "<p>Hallo " + name + ",</p>"
                        + "<p>Uw bestelling <strong>" + orderRef + "</strong> is klaar. Gelieve <strong>" + totalStr
                        + "</strong> klaar te hebben voor betaling ter plaatse.</p>"
                        + "<p style=\"margin:0 0 28px;text-align:center;\"><span style=\"display:inline-block;padding:14px;background:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;\">"
                        + "<img src=\"" + qrImgUrl
                        + "\" width=\"220\" height=\"220\" alt=\"QR-code bestelling\" style=\"display:block;border-radius:6px;margin:0 auto;\"/></span></p>"
                        + generateButton("Mijn bestelling volgen", clientOrdersUrl);
            }
            case "DE" -> {
                titleOut[0] = "Bestellbestätigung";
                inner = "<p>Hallo " + name + ",</p>"
                        + "<p>Ihre Bestellung <strong>" + orderRef + "</strong> ist fertig. Bitte halten Sie <strong>" + totalStr
                        + "</strong> für die Zahlung vor Ort bereit.</p>"
                        + "<p style=\"margin:0 0 28px;text-align:center;\"><span style=\"display:inline-block;padding:14px;background:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;\">"
                        + "<img src=\"" + qrImgUrl
                        + "\" width=\"220\" height=\"220\" alt=\"Bestell-QR-Code\" style=\"display:block;border-radius:6px;margin:0 auto;\"/></span></p>"
                        + generateButton("Meine Bestellung verfolgen", clientOrdersUrl);
            }
            default -> {
                titleOut[0] = "Confirmation de votre commande";
                inner = "<p>Bonjour " + name + ",</p>"
                        + "<p>Votre commande <strong>" + orderRef + "</strong> est prête. Merci de préparer la somme de <strong>"
                        + totalStr + "</strong>.</p>"
                        + "<p style=\"margin:0 0 28px;text-align:center;\"><span style=\"display:inline-block;padding:14px;background:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;\">"
                        + "<img src=\"" + qrImgUrl
                        + "\" width=\"220\" height=\"220\" alt=\"QR Code commande\" style=\"display:block;border-radius:6px;margin:0 auto;\"/></span></p>"
                        + generateButton("Suivre ma commande", clientOrdersUrl);
            }
        }
        return inner;
    }

    public void sendWelcomeEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            String rendered = emailTemplateService.renderWelcome(user);
            String title = extractFirstH1PlainText(rendered);
            String inner = stripFirstH1(rendered);
            if (title.isBlank()) {
                title = emailTemplateService.welcomeSubject(user);
            }
            String html = toHtmlDocument(wrapInMasterTemplate(title, inner));
            String subject = emailTemplateService.welcomeSubject(user);
            mailDispatchService.sendHtml(user.getEmail().trim(), subject, html);
        } catch (Exception e) {
            log.warn("Welcome email could not be sent to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendPasswordResetEmail(User user, String plainToken) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            String resetUrl = buildResetLink(plainToken);
            String[] titleHolder = new String[1];
            String innerContent = buildPasswordResetInnerAndTitle(user, resetUrl, titleHolder);
            String finalHtml = toHtmlDocument(wrapInMasterTemplate(titleHolder[0], innerContent));
            String subject = emailTemplateService.passwordResetSubject(user);
            mailDispatchService.sendHtml(user.getEmail().trim(), subject, finalHtml);
        } catch (Exception e) {
            log.warn("Password reset email could not be sent to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Value("${nhcwash.app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private String buildResetLink(String token) {
        return EmailTemplateService.normalizeFrontendBase(frontendBaseUrl) + "/reset-password?token=" + token;
    }

    /**
     * QR code + instructions after a pay-on-site checkout.
     */
    @Async
    public void sendPayOnSiteInstructions(Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            Order order = orderEmailQueryService.loadOrderForEmail(orderId).orElse(null);
            if (order == null) {
                return;
            }
            User client = order.getClient();
            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                return;
            }
            String[] titleHolder = new String[1];
            String innerContent = buildPayOnSiteInnerAndTitle(client, order, titleHolder);
            String finalHtml = toHtmlDocument(wrapInMasterTemplate(titleHolder[0], innerContent));
            String subject = emailTemplateService.payOnSiteSubject(client, orderId);
            mailDispatchService.sendHtml(client.getEmail().trim(), subject, finalHtml);
        } catch (Exception e) {
            log.warn("Pay-on-site email for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Confirmation after staff records an on-site payment (trigger after DB commit from {@link PaymentService}).
     */
    @Async
    public void sendPaymentReceipt(Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            Order order = orderEmailQueryService.loadOrderForEmail(orderId).orElse(null);
            if (order == null) {
                return;
            }
            User client = order.getClient();
            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                return;
            }
            String[] titleHolder = new String[1];
            String innerContent = buildPaymentReceiptInnerAndTitle(client, order, titleHolder);
            String finalHtml = toHtmlDocument(wrapInMasterTemplate(titleHolder[0], innerContent));
            String subject = emailTemplateService.paymentReceiptSubject(client, orderId);
            mailDispatchService.sendHtml(client.getEmail().trim(), subject, finalHtml);
        } catch (Exception e) {
            log.warn("Payment receipt email for order {}: {}", orderId, e.getMessage());
        }
    }

    public void sendContactInquiry(String adminEmail, ContactRequest request) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        try {
            String brand = "NHCWash";
            try {
                String n = globalSettingsService.getSettingsEntity().getCompanyName();
                if (n != null && !n.isBlank()) {
                    brand = n.trim();
                }
            } catch (Exception ignored) {
                /* keep default */
            }
            String mailSubject = "[" + brand + "] Nouveau message de contact : " + request.getSubject().trim();
            String name = request.getName().trim();
            String email = request.getEmail().trim();
            String subjectLine = request.getSubject().trim();
            String bodyText = request.getMessage().trim();

            String innerFragment = "<h1 style=\"margin:0 0 16px;font-size:22px;font-weight:700;color:#111827;\">Message de contact</h1>"
                    + "<p style=\"margin:0 0 8px;\"><strong>De :</strong> " + escapeHtml(name) + " &lt;" + escapeHtml(email) + "&gt;</p>"
                    + "<p style=\"margin:0 0 16px;\"><strong>Sujet :</strong> " + escapeHtml(subjectLine) + "</p>"
                    + "<div style=\"margin:0;padding:16px;background:#f8fafc;border-radius:8px;border:1px solid #e2e8f0;"
                    + "white-space:pre-wrap;font-size:15px;color:#334155;\">" + escapeHtml(bodyText) + "</div>";

            String title = extractFirstH1PlainText(innerFragment);
            String inner = stripFirstH1(innerFragment);
            mailDispatchService.sendHtml(adminEmail.trim(), mailSubject, toHtmlDocument(wrapInMasterTemplate(title, inner)), email);

            sendContactVisitorAcknowledgement(email, name, brand);
        } catch (Exception e) {
            log.warn("Contact inquiry could not be queued: {}", e.getMessage());
        }
    }

    private void sendContactVisitorAcknowledgement(String visitorEmail, String visitorName, String brand) {
        if (visitorEmail == null || visitorEmail.isBlank()) {
            return;
        }
        try {
            String first = visitorName != null && !visitorName.isBlank() ? escapeHtml(visitorName) : "";
            String greeting = first.isEmpty() ? "Bonjour," : ("Bonjour " + first + ",");
            String innerFragment = "<h1 style=\"margin:0 0 16px;font-size:22px;font-weight:700;color:#111827;\">Merci pour votre message</h1>"
                    + "<p style=\"margin:0 0 12px;\">" + greeting + "</p>"
                    + "<p style=\"margin:0 0 12px;\">Nous avons bien reçu votre demande concernant <strong>" + escapeHtml(brand)
                    + "</strong>. Notre équipe vous répondra dans les meilleurs délais.</p>"
                    + "<p style=\"margin:0;color:#64748b;font-size:14px;\">Ceci est un accusé de réception automatique.</p>";
            String title = extractFirstH1PlainText(innerFragment);
            String inner = stripFirstH1(innerFragment);
            mailDispatchService.sendHtml(visitorEmail.trim(), brand + " — Accusé de réception",
                    toHtmlDocument(wrapInMasterTemplate(title, inner)));
        } catch (Exception e) {
            log.warn("Contact auto-reply could not be sent to {}: {}", visitorEmail, e.getMessage());
        }
    }

    /**
     * Sent after admin archives an account or after client GDPR self-delete.
     * Uses blocking SMTP so template/transport errors are visible on the caller thread (not swallowed by {@code @Async}).
     *
     * @param deletedByAdmin {@code true} when an administrator archived the account; {@code false} for client self-delete.
     */
    public void sendAccountDeletedEmail(User user, boolean deletedByAdmin) {
        try {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }
            log.info("Triggering deletion email for: {} (deletedByAdmin={})", user.getEmail().trim(), deletedByAdmin);
            String rendered = emailTemplateService.renderAccountDeleted(user, deletedByAdmin);
            String title = extractFirstH1PlainText(rendered);
            String inner = stripFirstH1(rendered);
            if (title.isBlank()) {
                title = emailTemplateService.accountDeletedSubject(user, deletedByAdmin);
            }
            String html = toHtmlDocument(wrapInMasterTemplate(title, inner));
            String subject = emailTemplateService.accountDeletedSubject(user, deletedByAdmin);
            mailDispatchService.sendHtmlBlocking(user.getEmail().trim(), subject, html);
        } catch (Exception e) {
            System.err.println("====== EMAIL FAILED TO SEND ======");
            e.printStackTrace();
            log.error("Account deleted email could not be sent for user id={} email={}: {}",
                    user != null ? user.getUserId() : null,
                    user != null ? user.getEmail() : null,
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Sent after an admin restores a soft-deleted client account ({@code POST /api/admin/users/{id}/restore}).
     */
    @Async
    public void sendAccountRestoredEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            String rendered = emailTemplateService.renderAccountRestored(user);
            String title = extractFirstH1PlainText(rendered);
            String inner = stripFirstH1(rendered);
            if (title.isBlank()) {
                title = emailTemplateService.accountRestoredSubject(user);
            }
            String html = toHtmlDocument(wrapInMasterTemplate(title, inner));
            String subject = emailTemplateService.accountRestoredSubject(user);
            mailDispatchService.sendHtml(user.getEmail().trim(), subject, html);
        } catch (Exception e) {
            log.warn("Account restored email could not be sent to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Queues refund confirmation after the current transaction commits, then reloads the order so billing data
     * (payments REFUNDED, client) is visible to the mail renderer (avoids empty sends with {@code @Async} before commit).
     */
    public void sendRefundConfirmation(Order order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        Long orderId = order.getOrderId();
        Runnable send = () -> sendRefundConfirmationAfterLoad(orderId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void sendRefundConfirmationAfterLoad(Long orderId) {
        try {
            Order loaded = orderEmailQueryService.loadOrderForEmail(orderId).orElse(null);
            if (loaded == null) {
                return;
            }
            User client = loaded.getClient();
            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                return;
            }
            BigDecimal refundAmount = resolveRefundAmountForConfirmation(loaded);
            String rendered = emailTemplateService.renderRefundConfirmation(client, loaded, refundAmount);
            String title = extractFirstH1PlainText(rendered);
            String inner = stripFirstH1(rendered);
            if (title.isBlank()) {
                title = emailTemplateService.refundConfirmationSubject(client);
            }
            String html = toHtmlDocument(wrapInMasterTemplate(title, inner));
            String subject = emailTemplateService.refundConfirmationSubject(client);
            mailDispatchService.sendHtml(client.getEmail().trim(), subject, html);
        } catch (Exception e) {
            log.warn("Refund confirmation email for order {}: {}", orderId, e.getMessage());
        }
    }

    private static BigDecimal resolveRefundAmountForConfirmation(Order loaded) {
        if (loaded.getPayments() != null && !loaded.getPayments().isEmpty()) {
            BigDecimal fromRefunded = loaded.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                    .max(Comparator.comparing(Payment::getPaymentId, Comparator.nullsFirst(Long::compareTo)))
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .orElse(null);
            if (fromRefunded != null && fromRefunded.compareTo(BigDecimal.ZERO) > 0) {
                return fromRefunded;
            }
        }
        if (loaded.getTotalAmount() != null) {
            return loaded.getTotalAmount();
        }
        return BigDecimal.ZERO;
    }
}
