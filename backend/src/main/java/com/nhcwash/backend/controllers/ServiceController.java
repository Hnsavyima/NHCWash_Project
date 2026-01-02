package com.nhcwash.backend.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.models.dtos.ServiceDTO;
import com.nhcwash.backend.models.dtos.DTOConvertor.DtoConverter;
import com.nhcwash.backend.repositories.ServiceRepository;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    @Autowired
    private ServiceRepository repo;
    @Autowired
    private DtoConverter converter;

    @GetMapping
    public List<ServiceDTO> getAll(@RequestParam(defaultValue = "fr") String lang) {
        return repo.findAll().stream()
                .map(s -> converter.toServiceDto(s, lang))
                .collect(Collectors.toList());
    }
}