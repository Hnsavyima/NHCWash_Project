package com.nhcwash.backend.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.UserAddressRequestDTO;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.entities.UserAddress;
import com.nhcwash.backend.repositories.AppointmentRepository;
import com.nhcwash.backend.repositories.UserAddressRepository;
import com.nhcwash.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<UserAddress> listAddressesForUser(Long userId) {
        return userAddressRepository.findByUser_UserIdOrderByIsDefaultDescAddressIdAsc(userId);
    }

    @Transactional
    public UserAddress createAddress(Long userId, UserAddressRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        boolean firstAddress = userAddressRepository.findByUser_UserId(userId).isEmpty();
        boolean makeDefault = Boolean.TRUE.equals(dto.getDefaultAddress()) || firstAddress;

        if (makeDefault) {
            clearDefaultsForUser(userId, null);
        }

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setLabel(trimToNull(dto.getLabel()));
        address.setStreet(dto.getStreet().trim());
        address.setNumber(trimToNull(dto.getNumber()));
        address.setBox(trimToNull(dto.getBox()));
        address.setPostalCode(trimToNull(dto.getPostalCode()));
        address.setCity(dto.getCity().trim());
        address.setCountry(dto.getCountry().trim());
        address.setIsDefault(makeDefault);

        return userAddressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        UserAddress address = getAddressForUserOrThrow(addressId, userId);

        if (appointmentRepository.countByAddress_AddressId(addressId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer une adresse utilisée par un rendez-vous");
        }

        userAddressRepository.delete(address);
    }

    @Transactional
    public UserAddress setDefaultAddress(Long addressId, Long userId) {
        UserAddress address = getAddressForUserOrThrow(addressId, userId);

        clearDefaultsForUser(userId, address.getAddressId());
        address.setIsDefault(true);

        return userAddressRepository.save(address);
    }

    private UserAddress getAddressForUserOrThrow(Long addressId, Long userId) {
        return userAddressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> {
                    if (userAddressRepository.existsById(addressId)) {
                        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Cette adresse n'appartient pas à l'utilisateur");
                    }
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Adresse introuvable");
                });
    }

    private void clearDefaultsForUser(Long userId, Long exceptAddressId) {
        List<UserAddress> all = userAddressRepository.findByUser_UserId(userId);
        for (UserAddress a : all) {
            if (exceptAddressId == null || !a.getAddressId().equals(exceptAddressId)) {
                a.setIsDefault(false);
            }
        }
        userAddressRepository.saveAll(all);
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
