package com.nhcwash.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {
}
