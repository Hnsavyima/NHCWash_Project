package com.nhcwash.backend.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.models.dtos.ServiceDTO;
import com.nhcwash.backend.repositories.ServiceRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final DtoConverter converter;

    @GetMapping
    public List<ServiceDTO> getAll(@RequestParam(defaultValue = "fr") String lang) {
        return serviceRepository.findAll().stream()
                .map(s -> converter.toServiceDto(s, lang))
                .collect(Collectors.toList());
    }
}
