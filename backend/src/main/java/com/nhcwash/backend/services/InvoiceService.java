package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.entities.Invoice;
import com.nhcwash.backend.models.entities.InvoiceLine;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.repositories.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final BigDecimal DEFAULT_VAT_RATE_PERCENT = BigDecimal.ZERO;

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoiceForOrder(Order order) {
        if (invoiceRepository.findByOrder_OrderId(order.getOrderId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une facture existe déjà pour cette commande");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceLine> lines = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            InvoiceLine line = new InvoiceLine();
            String label = item.getArticleType();
            if (label == null && item.getService() != null) {
                label = item.getService().getName();
            }
            if (label == null) {
                label = "Article";
            }
            line.setLabel(label);
            line.setQuantity(item.getQuantity());

            BigDecimal unit = item.getUnitPriceEstimated() != null ? item.getUnitPriceEstimated() : BigDecimal.ZERO;
            BigDecimal lineTotal = item.getLineTotalEstimated() != null
                    ? item.getLineTotalEstimated()
                    : unit.multiply(BigDecimal.valueOf(item.getQuantity()));
            line.setUnitPrice(unit);
            line.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
            lines.add(line);
        }

        BigDecimal vatRate = DEFAULT_VAT_RATE_PERCENT;
        BigDecimal vatAmount = subtotal.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vatAmount);

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber("INV-" + order.getOrderId());
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setVatRate(vatRate);
        invoice.setSubtotal(subtotal);
        invoice.setVatAmount(vatAmount);
        invoice.setTotal(total);

        for (InvoiceLine line : lines) {
            line.setInvoice(invoice);
        }
        invoice.setLines(lines);

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceByIdForUser(Long invoiceId, Long userId) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
        if (!invoice.getOrder().getClient().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à cette facture");
        }
        return invoice;
    }
}
