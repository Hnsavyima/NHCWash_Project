package com.nhcwash.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nhcwash.backend.models.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = { "role" })
    @Query("select u from User u order by u.userId")
    List<User> findAllWithRoleForExport();

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email); // Très important pour l'inscription

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndUserIdNot(String phone, Long userId);
}
