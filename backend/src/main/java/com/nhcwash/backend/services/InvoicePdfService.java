package com.nhcwash.backend.services;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.nhcwash.backend.models.entities.GlobalSettings;
import com.nhcwash.backend.models.entities.Invoice;
import com.nhcwash.backend.models.entities.InvoiceLine;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.OrderStatus;
import com.nhcwash.backend.models.enumerations.PaymentMethod;
import com.nhcwash.backend.models.enumerations.PaymentStatus;
import com.nhcwash.backend.repositories.InvoiceRepository;
import com.nhcwash.backend.repositories.OrderRepository;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter ISSUED_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd MMMM yyyy", Locale.FRENCH);

    /** Wording aligned with frontend {@code fr.json} {@code invoices.pdf.*} (PDF is French-only). */
    private static final String TAGLINE = "Pressing & blanchisserie";
    private static final String COMPANY_ADDRESS_HTML_FALLBACK = "Service à domicile — Bruxelles et périphérie<br/>Belgique";
    private static final String COMPANY_VAT_FALLBACK = "N° TVA : BE 0000.000.000";
    private static final String DOCUMENT_TITLE = "FACTURE";
    private static final String BILL_TO = "Facturé à";
    private static final String META_ORDER = "Commande";
    private static final String META_DATE = "Date";
    private static final String META_STATUS = "Statut commande";
    private static final String META_PAYMENT = "Paiement";
    private static final String META_PAYMENT_MEANS = "Moyen de paiement";
    private static final String META_PAYMENT_REF = "Réf. transaction";
    private static final String META_ISSUED = "Émis le";
    private static final String LINES_SECTION = "Détail des prestations";
    private static final String COL_SERVICE = "Service";
    private static final String COL_QTY = "Qté";
    private static final String COL_UNIT = "Prix unit.";
    private static final String COL_LINE = "Total ligne";
    private static final String SUBTOTAL_HT = "Sous-total HT";
    private static final String TOTAL_TTC = "Total TTC";
    private static final String FOOTER_FALLBACK = "NHCWash — Merci de votre confiance. En cas de question, contactez-nous via le site.";

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final InvoiceService invoiceService;
    private final SpringTemplateEngine pdfTemplateEngine;
    private final GlobalSettingsService globalSettingsService;

    public InvoicePdfService(InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            InvoiceService invoiceService,
            @Qualifier("pdfTemplateEngine") SpringTemplateEngine pdfTemplateEngine,
            GlobalSettingsService globalSettingsService) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.invoiceService = invoiceService;
        this.pdfTemplateEngine = pdfTemplateEngine;
        this.globalSettingsService = globalSettingsService;
    }

    /**
     * Client PDF invoices: one Thymeleaf template. Creates a missing {@link Invoice} for legacy or manual flows.
     * Refunded and cancelled (but invoiced) orders keep access — legal retention of the original invoice.
     */
    @Transactional
    public byte[] generatePaidOrderInvoicePdf(Order order) {
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande invalide");
        }
        Order full = orderRepository.findWithDetailsById(order.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
        if (!allowsInvoiceDocument(full)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune facture disponible pour cette commande");
        }
        Invoice invoice = invoiceRepository.findByOrder_OrderId(full.getOrderId())
                .orElseGet(() -> invoiceService.findOrCreateInvoiceForOrder(full));
        return buildInvoicePdf(full, invoice);
    }

    /**
     * True when an invoice exists, was refunded, is paid/delivered, or has a succeeded/refunded payment row.
     */
    private boolean allowsInvoiceDocument(Order o) {
        if (invoiceRepository.findByOrder_OrderId(o.getOrderId()).isPresent()) {
            return true;
        }
        if (o.getRefundDate() != null) {
            return true;
        }
        if (o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.DELIVERED) {
            return true;
        }
        if (o.getPayments() != null) {
            return o.getPayments().stream()
                    .anyMatch(p -> p.getStatus() == PaymentStatus.SUCCEEDED
                            || p.getStatus() == PaymentStatus.REFUNDED);
        }
        return false;
    }

    /** Prefer succeeded payment for display; else latest refunded payment (original invoice still valid). */
    private static Payment findDisplayPayment(Order order) {
        if (order.getPayments() == null || order.getPayments().isEmpty()) {
            return null;
        }
        return order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .max(Comparator.comparing(Payment::getPaymentId, Comparator.nullsFirst(Long::compareTo)))
                .orElseGet(() -> order.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                        .max(Comparator.comparing(Payment::getPaymentId, Comparator.nullsFirst(Long::compareTo)))
                        .orElse(null));
    }

    private byte[] buildInvoicePdf(Order order, Invoice invoice) {
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commande invalide");
        }
        if (invoice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Facture introuvable");
        }

        User client = order.getClient();
        String customerName = "";
        String customerEmail = "";
        if (client != null) {
            String fn = client.getFirstName() != null ? client.getFirstName() : "";
            String ln = client.getLastName() != null ? client.getLastName() : "";
            customerName = (fn + " " + ln).trim();
            customerEmail = client.getEmail() != null ? client.getEmail() : "";
        }

        List<InvoicePdfLine> lines = new ArrayList<>();
        if (invoice.getLines() != null) {
            for (InvoiceLine line : invoice.getLines()) {
                lines.add(new InvoicePdfLine(
                        line.getLabel() != null ? line.getLabel() : "—",
                        line.getQuantity() != null ? line.getQuantity() : 0,
                        line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO,
                        line.getLineTotal() != null ? line.getLineTotal() : BigDecimal.ZERO));
            }
        }

        BigDecimal subtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal vatRate = invoice.getVatRate() != null ? invoice.getVatRate() : BigDecimal.ZERO;
        BigDecimal vatAmount = invoice.getVatAmount() != null ? invoice.getVatAmount() : BigDecimal.ZERO;
        BigDecimal total = invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO;

        String vatLineLabel = "TVA (" + formatRatePlain(vatRate) + " %)";

        GlobalSettings site = safeSettings();

        Context ctx = new Context();
        ctx.setVariable("companyName", site.getCompanyName());
        ctx.setVariable("tagline", TAGLINE);
        ctx.setVariable("companyAddressHtml", addressToHtml(site.getAddress()));
        ctx.setVariable("companyVat", formatVatLine(site.getVatNumber()));
        ctx.setVariable("documentTitle", DOCUMENT_TITLE);
        ctx.setVariable("invoiceNumber", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "—");
        ctx.setVariable("orderRef", String.format("CMD-%03d", order.getOrderId()));
        ctx.setVariable("issuedAt",
                invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(ISSUED_FORMAT) : "—");
        ctx.setVariable("orderDateStr",
                order.getCreatedAt() != null ? ORDER_DATE_FORMAT.format(order.getCreatedAt()) : "—");
        ctx.setVariable("customerName", customerName.isEmpty() ? "—" : customerName);
        ctx.setVariable("customerEmail", customerEmail.isEmpty() ? "—" : customerEmail);
        ctx.setVariable("billToLabel", BILL_TO);
        ctx.setVariable("metaOrderRefLabel", META_ORDER);
        ctx.setVariable("metaDateLabel", META_DATE);
        ctx.setVariable("metaStatusLabel", META_STATUS);
        ctx.setVariable("metaPaymentLabel", META_PAYMENT);
        ctx.setVariable("metaPaymentMeansLabel", META_PAYMENT_MEANS);
        ctx.setVariable("metaPaymentRefLabel", META_PAYMENT_REF);
        ctx.setVariable("metaIssuedLabel", META_ISSUED);
        ctx.setVariable("orderStatusLabel", formatOrderStatusFr(order.getStatus()));
        Payment displayPayment = findDisplayPayment(order);
        ctx.setVariable("paymentStatusLabel", formatPaymentStatusFr(order, displayPayment));
        fillOptionalPaymentLines(ctx, order, displayPayment);
        ctx.setVariable("linesSectionLabel", LINES_SECTION);
        ctx.setVariable("colService", COL_SERVICE);
        ctx.setVariable("colQty", COL_QTY);
        ctx.setVariable("colUnit", COL_UNIT);
        ctx.setVariable("colLineTotal", COL_LINE);
        ctx.setVariable("lines", lines);
        ctx.setVariable("subtotal", subtotal);
        ctx.setVariable("vatRate", vatRate);
        ctx.setVariable("vatAmount", vatAmount);
        ctx.setVariable("total", total);
        ctx.setVariable("subtotalExVatLabel", SUBTOTAL_HT);
        ctx.setVariable("vatLineLabel", vatLineLabel);
        ctx.setVariable("totalTtcLabel", TOTAL_TTC);
        ctx.setVariable("footerNote", buildFooterNote(site));

        String html = pdfTemplateEngine.process("invoice", ctx);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html, null);
            renderer.layout();
            renderer.createPDF(baos);
            renderer.finishPDF();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de générer le PDF : " + e.getMessage());
        }
    }

    private static String formatRatePlain(BigDecimal rate) {
        if (rate == null) {
            return "0";
        }
        return rate.stripTrailingZeros().toPlainString();
    }

    private static String formatOrderStatusFr(OrderStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case PENDING -> "En attente";
            case PAID -> "Payée";
            case RECEIVED -> "Reçue";
            case PROCESSING -> "En traitement";
            case READY -> "Prête";
            case DELIVERED -> "Livrée";
            case CANCELLED -> "Annulée";
        };
    }

    private static String formatPaymentStatusFr(Order order, Payment p) {
        if (p != null) {
            if (p.getStatus() == PaymentStatus.REFUNDED) {
                return "Remboursé";
            }
            if (p.getStatus() == PaymentStatus.SUCCEEDED) {
                return switch (p.getMethod()) {
                    case CASH -> "Payé — espèces (sur place)";
                    case POS_TERMINAL -> "Payé — terminal CB (sur place)";
                    case CARD -> "Payé — carte (en ligne)";
                };
            }
        }
        if (order.getRefundDate() != null) {
            return "Remboursé";
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED) {
            return "Payée";
        }
        return "En attente";
    }

    private static void fillOptionalPaymentLines(Context ctx, Order order, Payment p) {
        if (p != null) {
            String meansText = null;
            if (p.getMethod() == PaymentMethod.CASH) {
                meansText = "Paiement manuel / espèces";
            } else if (p.getMethod() == PaymentMethod.POS_TERMINAL) {
                meansText = "Paiement manuel / terminal CB";
            } else if (p.getMethod() == PaymentMethod.CARD) {
                meansText = "Carte bancaire (paiement en ligne)";
            }
            ctx.setVariable("paymentMeansVisible", meansText != null);
            ctx.setVariable("paymentMeansText", meansText != null ? meansText : "");
            String ref = p.getProviderTxId();
            boolean hasRef = ref != null && !ref.isBlank();
            ctx.setVariable("paymentRefVisible", hasRef);
            ctx.setVariable("paymentRefText", hasRef ? ref : "");
            return;
        }
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED) {
            ctx.setVariable("paymentMeansVisible", true);
            ctx.setVariable("paymentMeansText", "Encaissement enregistré (hors ligne)");
            ctx.setVariable("paymentRefVisible", false);
            ctx.setVariable("paymentRefText", "");
            return;
        }
        if (order.getRefundDate() != null) {
            ctx.setVariable("paymentMeansVisible", true);
            ctx.setVariable("paymentMeansText", "Remboursement enregistré");
            ctx.setVariable("paymentRefVisible", false);
            ctx.setVariable("paymentRefText", "");
            return;
        }
        ctx.setVariable("paymentMeansVisible", false);
        ctx.setVariable("paymentMeansText", "");
        ctx.setVariable("paymentRefVisible", false);
        ctx.setVariable("paymentRefText", "");
    }

    private GlobalSettings safeSettings() {
        try {
            return globalSettingsService.getSettingsEntity();
        } catch (Exception e) {
            GlobalSettings g = new GlobalSettings();
            g.setCompanyName("NHCWash");
            g.setContactEmail("contact@nhcwash.be");
            g.setContactPhone("+32 2 123 45 67");
            g.setAddress("Bruxelles et périphérie, Belgique");
            g.setVatNumber("BE 0000.000.000");
            g.setOpeningHoursDescription("");
            g.setSupportEmail("contact@nhcwash.be");
            return g;
        }
    }

    private static String addressToHtml(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMPANY_ADDRESS_HTML_FALLBACK;
        }
        return HtmlUtils.htmlEscape(raw).replace("\n", "<br/>");
    }

    private static String formatVatLine(String vat) {
        if (vat == null || vat.isBlank()) {
            return COMPANY_VAT_FALLBACK;
        }
        String t = vat.trim();
        String lower = t.toLowerCase();
        if (lower.contains("tva") || lower.contains("btw") || lower.contains("vat")) {
            return HtmlUtils.htmlEscape(t);
        }
        return "N° TVA : " + HtmlUtils.htmlEscape(t);
    }

    private static String buildFooterNote(GlobalSettings site) {
        String name = site.getCompanyName() != null ? site.getCompanyName().trim() : "";
        String support = site.getSupportEmail() != null ? site.getSupportEmail().trim() : "";
        if (name.isEmpty() && support.isEmpty()) {
            return FOOTER_FALLBACK;
        }
        if (name.isEmpty()) {
            name = "NHCWash";
        }
        if (support.isEmpty()) {
            support = site.getContactEmail() != null ? site.getContactEmail().trim() : "";
        }
        return name + " — Merci de votre confiance. Questions : " + support;
    }

    /** Line model exposed to Thymeleaf (getters used by OGNL). */
    public static final class InvoicePdfLine {
        private final String label;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal lineTotal;

        public InvoicePdfLine(String label, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
            this.label = label;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        public String getLabel() {
            return label;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }
    }
}
