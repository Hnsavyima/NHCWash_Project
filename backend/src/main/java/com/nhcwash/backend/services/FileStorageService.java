package com.nhcwash.backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final String PUBLIC_PREFIX = "/uploads/avatars/";

    @Value("${nhcwash.upload.dir:uploads}")
    private String uploadDir;

    public Path avatarsDirectory() {
        return Paths.get(uploadDir).resolve("avatars").toAbsolutePath().normalize();
    }

    /**
     * Stores an avatar under {@code uploads/avatars/} with a unique name.
     *
     * @return public path for static serving, e.g. {@code /uploads/avatars/uuid.jpg}
     */
    public String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier avatar vide");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image trop volumineuse (max 2 Mo)");
        }
        String ext = resolveExtension(file);
        if (ext == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format d'image non supporté (JPEG, PNG, GIF, WebP)");
        }
        String filename = UUID.randomUUID() + ext;
        try {
            Path dir = avatarsDirectory();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return PUBLIC_PREFIX + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer l'avatar");
        }
    }

    /**
     * Deletes a stored avatar file from disk when {@code fileUrl} is a safe path under
     * {@code /uploads/avatars/}. No-op for null, blank, or non-managed URLs.
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String name = fileUrl.substring(PUBLIC_PREFIX.length());
        if (name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return;
        }
        Path dir = avatarsDirectory();
        Path target = dir.resolve(name).normalize();
        if (!target.startsWith(dir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private String resolveExtension(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null) {
            String lower = ct.toLowerCase(Locale.ROOT);
            if (lower.contains(MediaType.IMAGE_JPEG_VALUE)) {
                return ".jpg";
            }
            if (lower.contains("image/png")) {
                return ".png";
            }
            if (lower.contains("image/gif")) {
                return ".gif";
            }
            if (lower.contains("image/webp")) {
                return ".webp";
            }
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String ext = name.substring(dot).toLowerCase(Locale.ROOT);
                if (ALLOWED_EXT.contains(ext)) {
                    return ext.equals(".jpeg") ? ".jpg" : ext;
                }
            }
        }
        return null;
    }
}
