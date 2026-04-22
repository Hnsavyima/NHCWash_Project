package com.nhcwash.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhcwash.backend.models.entities.GlobalSettings;

@Repository
public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, Long> {
}
