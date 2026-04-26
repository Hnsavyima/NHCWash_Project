package com.nhcwash.backend.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.constants.PasswordRules;
import com.nhcwash.backend.models.entities.PasswordResetToken;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.PasswordResetTokenRepository;
import com.nhcwash.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_VALID_HOURS = 24;

    private static final Pattern PASSWORD_POLICY = Pattern.compile(PasswordRules.PATTERN);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    /**
     * Creates a reset token and sends email if the user exists. Always safe to call (no email enumeration).
     */
    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim();
        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getIsActive())) {
            return;
        }

        tokenRepository.deleteByUserId(user.getUserId());

        String plainToken = UUID.randomUUID().toString();
        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setToken(plainToken);
        entity.setExpiresAt(Instant.now().plus(TOKEN_VALID_HOURS, ChronoUnit.HOURS));
        entity.setUsed(false);
        tokenRepository.save(entity);

        mailService.sendPasswordResetEmail(user, plainToken);
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jeton manquant");
        }
        if (newPassword == null || !PASSWORD_POLICY.matcher(newPassword).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordRules.MESSAGE);
        }

        PasswordResetToken prt = tokenRepository.findByTokenAndUsedIsFalse(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jeton invalide ou expiré"));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jeton expiré");
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);
    }
}
