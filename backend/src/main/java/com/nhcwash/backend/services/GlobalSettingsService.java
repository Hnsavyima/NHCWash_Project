package com.nhcwash.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhcwash.backend.models.dtos.GlobalSettingsDto;
import com.nhcwash.backend.models.entities.GlobalSettings;
import com.nhcwash.backend.repositories.GlobalSettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GlobalSettingsService {

    private final GlobalSettingsRepository repository;

    @Transactional(readOnly = true)
    public GlobalSettingsDto getSettingsDto() {
        return toDto(ensureRow());
    }

    @Transactional(readOnly = true)
    public GlobalSettings getSettingsEntity() {
        return ensureRow();
    }

    @Transactional
    public GlobalSettingsDto updateSettings(GlobalSettingsDto dto) {
        GlobalSettings g = ensureRowMutable();
        applyDto(g, dto);
        return toDto(repository.save(g));
    }

    private GlobalSettings ensureRow() {
        return repository.findById(GlobalSettings.SINGLETON_ID).orElseGet(this::insertDefaults);
    }

    private GlobalSettings ensureRowMutable() {
        return repository.findById(GlobalSettings.SINGLETON_ID).orElseGet(this::insertDefaults);
    }

    private GlobalSettings insertDefaults() {
        GlobalSettings g = defaultEntity();
        return repository.save(g);
    }

    private static GlobalSettings defaultEntity() {
        GlobalSettings g = new GlobalSettings();
        g.setId(GlobalSettings.SINGLETON_ID);
        g.setCompanyName("NHCWash");
        g.setContactEmail("contact@nhcwash.be");
        g.setContactPhone("+32 2 123 45 67");
        g.setAddress("Bruxelles et périphérie, Belgique");
        g.setVatNumber("BE 0000.000.000");
        g.setOpeningHoursDescription("Lundi – vendredi : 9h – 18h\nSamedi : 9h – 13h");
        g.setSupportEmail("contact@nhcwash.be");
        return g;
    }

    private static void applyDto(GlobalSettings g, GlobalSettingsDto dto) {
        g.setCompanyName(trimToEmpty(dto.getCompanyName()));
        g.setContactEmail(trimToEmpty(dto.getContactEmail()));
        g.setContactPhone(trimToEmpty(dto.getContactPhone()));
        g.setAddress(trimToEmpty(dto.getAddress()));
        g.setVatNumber(trimToEmpty(dto.getVatNumber()));
        g.setOpeningHoursDescription(trimToEmpty(dto.getOpeningHoursDescription()));
        g.setSupportEmail(trimToEmpty(dto.getSupportEmail()));
    }

    private static String trimToEmpty(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    private static GlobalSettingsDto toDto(GlobalSettings g) {
        GlobalSettingsDto d = new GlobalSettingsDto();
        d.setCompanyName(g.getCompanyName());
        d.setContactEmail(g.getContactEmail());
        d.setContactPhone(g.getContactPhone());
        d.setAddress(g.getAddress());
        d.setVatNumber(g.getVatNumber());
        d.setOpeningHoursDescription(g.getOpeningHoursDescription());
        d.setSupportEmail(g.getSupportEmail());
        return d;
    }
}
