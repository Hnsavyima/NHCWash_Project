package com.nhcwash.backend.services;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.util.LanguagePreference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final OrderRepository orderRepository;
    private final EmailTemplateService emailTemplateService;
    private final MailDispatchService mailDispatchService;
    private final InvoicePdfService invoicePdfService;

    /**
     * Sends a client email after an operational status change (staff workflow).
     * Reloads the order after commit; mail is dispatched asynchronously.
     */
    @Transactional(readOnly = true)
    public void sendOrderStatusUpdateEmail(Long orderId) {
        Order order = orderRepository.findWithNotificationDetailsById(orderId).orElse(null);
        if (order == null) {
            log.debug("Order {} not found for status notification", orderId);
            return;
        }
        User client = order.getClient();
        if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
            return;
        }

        String statusMessage = localizedStatusMessage(client, order.getStatus());
        if (statusMessage == null) {
            return;
        }

        BigDecimal total = order.getTotalAmount();
        String amountDisplay = total != null && total.compareTo(BigDecimal.ZERO) > 0
                ? EmailTemplateService.formatEuroAmount(total) + " €"
                : "—";

        String inner = emailTemplateService.renderOrderStatusUpdate(client, statusMessage, order.getOrderId(),
                amountDisplay);
        String html = MailService.generateEmailWrapper(inner);
        String subject = emailTemplateService.orderStatusUpdateSubject(client, order.getStatus());
        String to = client.getEmail().trim();

        /* Bonus: attach invoice PDF when paid or delivered (requires succeeded payment + invoice). */
        boolean attachInvoice = order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.PAID;
        if (attachInvoice) {
            try {
                byte[] pdf = invoicePdfService.generatePaidOrderInvoicePdf(order);
                String filename = "facture-commande-" + order.getOrderId() + ".pdf";
                mailDispatchService.sendHtmlWithAttachment(to, subject, html, filename, pdf);
            } catch (Exception e) {
                log.debug("No invoice PDF attached for order {}: {}", orderId, e.getMessage());
                mailDispatchService.sendHtml(to, subject, html);
            }
        } else {
            mailDispatchService.sendHtml(to, subject, html);
        }
    }

    private static String localizedStatusMessage(User client, OrderStatus status) {
        if (status == null) {
            return null;
        }
        String lang = LanguagePreference.normalize(client.getPreferredLanguage());
        return switch (lang) {
            case "EN" -> statusMessageEn(status);
            case "NL" -> statusMessageNl(status);
            case "DE" -> statusMessageDe(status);
            default -> statusMessageFr(status);
        };
    }

    private static String statusMessageFr(OrderStatus status) {
        return switch (status) {
            case PROCESSING -> "Votre commande est en cours de traitement par nos experts.";
            case READY -> "Bonne nouvelle ! Votre linge est prêt et attend d'être livré.";
            case DELIVERED -> "Votre commande a été livrée. Merci de votre confiance !";
            case PAID -> "Votre commande est enregistrée comme payée.";
            default -> null;
        };
    }

    private static String statusMessageEn(OrderStatus status) {
        return switch (status) {
            case PROCESSING -> "Your order is now being processed by our team.";
            case READY -> "Good news! Your laundry is ready and waiting to be delivered.";
            case DELIVERED -> "Your order has been delivered. Thank you for your trust!";
            case PAID -> "Your order has been marked as paid.";
            default -> null;
        };
    }

    private static String statusMessageNl(OrderStatus status) {
        return switch (status) {
            case PROCESSING -> "Uw bestelling wordt nu verwerkt door onze experts.";
            case READY -> "Goed nieuws! Uw was is klaar en wacht op levering.";
            case DELIVERED -> "Uw bestelling is geleverd. Bedankt voor uw vertrouwen!";
            case PAID -> "Uw bestelling is geregistreerd als betaald.";
            default -> null;
        };
    }

    private static String statusMessageDe(OrderStatus status) {
        return switch (status) {
            case PROCESSING -> "Ihre Bestellung wird derzeit von unserem Team bearbeitet.";
            case READY -> "Gute Nachricht! Ihre Wäsche ist fertig und wartet auf die Lieferung.";
            case DELIVERED -> "Ihre Bestellung wurde geliefert. Vielen Dank für Ihr Vertrauen!";
            case PAID -> "Ihre Bestellung wurde als bezahlt markiert.";
            default -> null;
        };
    }
}
