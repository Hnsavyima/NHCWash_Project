package com.nhcwash.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email); // Très important pour l'inscription

    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
}
