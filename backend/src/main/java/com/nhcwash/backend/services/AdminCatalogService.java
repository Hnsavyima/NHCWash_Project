package com.nhcwash.backend.services;

import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.ServiceUpsertRequest;
import com.nhcwash.backend.models.entities.ServiceCategory;
import com.nhcwash.backend.repositories.ServiceCategoryRepository;
import com.nhcwash.backend.repositories.ServiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    @Transactional
    public com.nhcwash.backend.models.entities.Service setServiceActive(Long serviceId, boolean active) {
        com.nhcwash.backend.models.entities.Service s = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));
        s.setIsActive(active);
        return serviceRepository.save(s);
    }

    @Transactional
    public com.nhcwash.backend.models.entities.Service createService(ServiceUpsertRequest req) {
        ServiceCategory category = serviceCategoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie introuvable"));
        com.nhcwash.backend.models.entities.Service s = new com.nhcwash.backend.models.entities.Service();
        applyUpsert(s, category, req);
        return serviceRepository.save(s);
    }

    @Transactional
    public com.nhcwash.backend.models.entities.Service updateService(Long serviceId, ServiceUpsertRequest req) {
        com.nhcwash.backend.models.entities.Service s = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));
        ServiceCategory category = serviceCategoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie introuvable"));
        applyUpsert(s, category, req);
        return serviceRepository.save(s);
    }

    private void applyUpsert(com.nhcwash.backend.models.entities.Service s, ServiceCategory category, ServiceUpsertRequest req) {
        if (req.getBasePrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix ne peut pas être négatif");
        }
        s.setCategory(category);
        s.setName(req.getName().trim());
        String desc = req.getDescription();
        s.setDescription(desc != null && !desc.isBlank() ? desc.trim() : null);
        s.setBasePrice(req.getBasePrice().setScale(2, RoundingMode.HALF_UP));
        Integer delay = req.getEstimatedDelayHours();
        s.setEstimatedDelayHours(delay != null ? delay : 24);
        Boolean active = req.getActive();
        s.setIsActive(active != null ? active : Boolean.TRUE);
    }
}
