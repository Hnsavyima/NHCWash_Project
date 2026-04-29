package com.nhcwash.backend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nhcwash.backend.models.dtos.GlobalSettingsDto;
import com.nhcwash.backend.models.entities.Order;
import com.nhcwash.backend.models.entities.OrderItem;
import com.nhcwash.backend.models.entities.Payment;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.OrderRepository;
import com.nhcwash.backend.repositories.ServiceRepository;
import com.nhcwash.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupExportService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final GlobalSettingsService globalSettingsService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public byte[] exportPortableJson() throws Exception {
        ObjectMapper mapper = objectMapper.copy();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", "nhcwash-backup-v1");
        root.put("exportedAt", LocalDateTime.now().toString());

        List<Map<String, Object>> users = userRepository.findAllWithRoleForExport().stream()
                .map(this::userToMap)
                .collect(Collectors.toList());
        root.put("users", users);

        List<Map<String, Object>> orders = orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::orderToMap)
                .collect(Collectors.toList());
        root.put("orders", orders);

        List<Map<String, Object>> services = serviceRepository.findAllForExport().stream()
                .map(this::serviceToMap)
                .collect(Collectors.toList());
        root.put("services", services);

        GlobalSettingsDto settingsDto = globalSettingsService.getSettingsDto();
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("companyName", settingsDto.getCompanyName());
        settings.put("contactEmail", settingsDto.getContactEmail());
        settings.put("contactPhone", settingsDto.getContactPhone());
        settings.put("address", settingsDto.getAddress());
        settings.put("vatNumber", settingsDto.getVatNumber());
        settings.put("openingHoursDescription", settingsDto.getOpeningHoursDescription());
        settings.put("supportEmail", settingsDto.getSupportEmail());
        root.put("globalSettings", settings);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("email", u.getEmail());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole() != null ? u.getRole().getName() : null);
        m.put("isActive", u.getIsActive());
        m.put("isDeleted", u.getIsDeleted());
        m.put("deletedAt", u.getDeletedAt());
        m.put("preferredLanguage", u.getPreferredLanguage());
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("createdAt", u.getCreatedAt());
        m.put("updatedAt", u.getUpdatedAt());
        return m;
    }

    private Map<String, Object> orderToMap(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.getOrderId());
        if (o.getClient() != null) {
            m.put("clientUserId", o.getClient().getUserId());
            m.put("clientEmail", o.getClient().getEmail());
        }
        m.put("createdAt", o.getCreatedAt());
        m.put("updatedAt", o.getUpdatedAt());
        m.put("status", o.getStatus() != null ? o.getStatus().name() : null);
        m.put("checkoutPaymentMode", o.getCheckoutPaymentMode() != null ? o.getCheckoutPaymentMode().name() : null);
        m.put("estimatedTotal", decimalToPlain(o.getEstimatedTotal()));
        m.put("finalTotal", decimalToPlain(o.getFinalTotal()));
        m.put("instructions", o.getInstructions());

        List<Map<String, Object>> items = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem it : o.getItems()) {
                items.add(orderItemToMap(it));
            }
        }
        m.put("items", items);

        List<Map<String, Object>> payments = new ArrayList<>();
        if (o.getPayments() != null) {
            for (Payment p : o.getPayments()) {
                payments.add(paymentToMap(p));
            }
        }
        m.put("payments", payments);
        return m;
    }

    private static Map<String, Object> orderItemToMap(OrderItem it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderItemId", it.getOrderItemId());
        if (it.getService() != null) {
            m.put("serviceId", it.getService().getServiceId());
            m.put("serviceName", it.getService().getName());
        }
        m.put("articleType", it.getArticleType());
        m.put("quantity", it.getQuantity());
        m.put("unitPriceEstimated", decimalToPlain(it.getUnitPriceEstimated()));
        m.put("lineTotalEstimated", decimalToPlain(it.getLineTotalEstimated()));
        return m;
    }

    private static Map<String, Object> paymentToMap(Payment p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paymentId", p.getPaymentId());
        m.put("amount", decimalToPlain(p.getAmount()));
        m.put("currency", p.getCurrency());
        m.put("providerTxId", p.getProviderTxId());
        m.put("paidAt", p.getPaidAt());
        m.put("createdAt", p.getCreatedAt());
        m.put("provider", p.getProvider() != null ? p.getProvider().name() : null);
        m.put("method", p.getMethod() != null ? p.getMethod().name() : null);
        m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        return m;
    }

    private Map<String, Object> serviceToMap(com.nhcwash.backend.models.entities.Service s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serviceId", s.getServiceId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("basePrice", decimalToPlain(s.getBasePrice()));
        m.put("estimatedDelayHours", s.getEstimatedDelayHours());
        m.put("isActive", s.getIsActive());
        if (s.getCategory() != null) {
            m.put("categoryId", s.getCategory().getCategoryId());
            m.put("categoryName", s.getCategory().getName());
        }
        return m;
    }

    private static String decimalToPlain(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }
}
