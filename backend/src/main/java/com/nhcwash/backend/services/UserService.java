package com.nhcwash.backend.services;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.constants.RoleNames;
import com.nhcwash.backend.models.dtos.UserPatchDTO;
import com.nhcwash.backend.models.dtos.UserUpdateDTO;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.UserRepository;
import com.nhcwash.backend.util.LanguagePreference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MailService mailService;

    @Transactional
    public User updateProfile(Long userId, UserUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        String phone = dto.getPhone();
        if (phone != null && !phone.isBlank()) {
            phone = phone.trim();
            if (userRepository.existsByPhoneAndUserIdNot(phone, userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé");
            }
        } else {
            phone = null;
        }

        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setPhone(phone);
        if (dto.getPreferredLanguage() != null && !dto.getPreferredLanguage().isBlank()) {
            user.setPreferredLanguage(LanguagePreference.normalize(dto.getPreferredLanguage()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public User patchProfile(Long userId, UserPatchDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (dto.getFirstName() != null) {
            if (dto.getFirstName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prénom ne peut pas être vide");
            }
            user.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null) {
            if (dto.getLastName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom ne peut pas être vide");
            }
            user.setLastName(dto.getLastName().trim());
        }
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().isBlank() ? null : dto.getPhone().trim();
            if (phone != null && userRepository.existsByPhoneAndUserIdNot(phone, userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé");
            }
            user.setPhone(phone);
        }
        if (dto.getPreferredLanguage() != null && !dto.getPreferredLanguage().isBlank()) {
            user.setPreferredLanguage(LanguagePreference.normalize(dto.getPreferredLanguage()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public User setUserActive(Long userId, boolean active, Long actorUserId, String reason) {
        if (actorUserId != null && actorUserId.equals(userId) && !active) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot modify or delete your own admin account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (Boolean.TRUE.equals(user.getIsDeleted()) || user.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de modifier le statut d'un utilisateur archivé");
        }
        user.setIsActive(active);
        if (active) {
            user.setSuspensionReason(null);
        } else {
            String trimmed = reason == null ? "" : reason.trim();
            if (trimmed.length() > 2000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif ne peut pas dépasser 2000 caractères");
            }
            user.setSuspensionReason(trimmed.isEmpty() ? null : trimmed);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User softDeleteUser(Long userId, Long actorUserId) {
        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot modify or delete your own admin account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        user.setDeletedAt(LocalDateTime.now());
        user.setIsDeleted(true);
        user.setIsActive(false);
        user.setSuspensionReason(null);
        User saved = userRepository.save(user);
        Long deletedUserId = saved.getUserId();
        Runnable sendDeletedMail = () -> userRepository.findById(deletedUserId).ifPresent(u -> {
            System.out.println("====== TRIGGERING DELETION EMAIL FOR: " + u.getEmail() + " ======");
            mailService.sendAccountDeletedEmail(u, true);
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendDeletedMail.run();
                }
            });
        } else {
            sendDeletedMail.run();
        }
        return saved;
    }

    @Transactional
    public User restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        user.setIsActive(true);
        user.setSuspensionReason(null);
        User saved = userRepository.save(user);
        mailService.sendAccountRestoredEmail(saved);
        return saved;
    }

    /**
     * Client self-service GDPR soft delete: record kept; login blocked until admin restore.
     */
    @Transactional
    public void softDeleteMyAccountByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (user.getRole() == null || !RoleNames.CLIENT.equals(user.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux comptes client");
        }
        if (user.getDeletedAt() != null || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Ce compte a déjà été supprimé");
        }
        user.setDeletedAt(LocalDateTime.now());
        user.setIsDeleted(true);
        user.setIsActive(false);
        user.setSuspensionReason(null);
        userRepository.save(user);
        mailService.sendAccountDeletedEmail(user, false);
    }
}
