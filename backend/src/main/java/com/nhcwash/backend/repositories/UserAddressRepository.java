package com.nhcwash.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhcwash.backend.models.entities.UserAddress;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser_UserId(Long userId);

    List<UserAddress> findByUser_UserIdOrderByIsDefaultDescAddressIdAsc(Long userId);

    Optional<UserAddress> findByAddressIdAndUser_UserId(Long addressId, Long userId);
}
