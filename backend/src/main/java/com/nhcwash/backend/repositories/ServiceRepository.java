package com.nhcwash.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nhcwash.backend.models.entities.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    @EntityGraph(attributePaths = { "category" })
    @Query("select s from Service s order by s.serviceId")
    List<Service> findAllForExport();
}

