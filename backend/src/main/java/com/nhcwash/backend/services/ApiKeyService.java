package com.nhcwash.backend.services;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nhcwash.backend.models.dtos.ApiKeyListDto;
import com.nhcwash.backend.models.dtos.CreateApiKeyRequest;
import com.nhcwash.backend.models.dtos.CreateApiKeyResponseDto;
import com.nhcwash.backend.models.entities.ApiKey;
import com.nhcwash.backend.repositories.ApiKeyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "sk_nhc_";
    private static final int RANDOM_BYTES = 24;
    private static final int PREVIEW_LEN = 12;

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<ApiKeyListDto> listKeys() {
        return apiKeyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CreateApiKeyResponseDto createKey(CreateApiKeyRequest request) {
        String raw = generateSecret();
        String preview = raw.length() <= PREVIEW_LEN ? raw : raw.substring(0, PREVIEW_LEN);

        ApiKey entity = new ApiKey();
        entity.setName(request.getName().trim());
        entity.setKeyHash(passwordEncoder.encode(raw));
        entity.setKeyPreview(preview);
        entity.setIsActive(true);
        ApiKey saved = apiKeyRepository.save(entity);

        CreateApiKeyResponseDto dto = new CreateApiKeyResponseDto();
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        dto.setKey(raw);
        dto.setCreatedAt(saved.getCreatedAt());
        return dto;
    }

    @Transactional
    public void deleteKey(Long id) {
        if (id == null || !apiKeyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clé introuvable");
        }
        apiKeyRepository.deleteById(id);
    }

    private String generateSecret() {
        byte[] buf = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(buf);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        return KEY_PREFIX + body;
    }

    private ApiKeyListDto toListDto(ApiKey k) {
        ApiKeyListDto d = new ApiKeyListDto();
        d.setId(k.getId());
        d.setName(k.getName());
        d.setMaskedKey(mask(k.getKeyPreview()));
        d.setCreatedAt(k.getCreatedAt());
        d.setActive(Boolean.TRUE.equals(k.getIsActive()));
        return d;
    }

    private static String mask(String preview) {
        if (preview == null || preview.isBlank()) {
            return "sk_••••••••";
        }
        return preview + "••••••••";
    }
}
