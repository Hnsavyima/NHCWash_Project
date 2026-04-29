package com.nhcwash.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhcwash.backend.services.BackupExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/backup")
@Tag(name = "Admin — Sauvegarde", description = "Export des données")
@RequiredArgsConstructor
public class AdminBackupController {

    private final BackupExportService backupExportService;

    @GetMapping("/export")
    @Operation(summary = "Exporter les données en JSON")
    public ResponseEntity<byte[]> exportJson() throws Exception {
        byte[] body = backupExportService.exportPortableJson();
        String filename = "nhcwash-export-" + LocalDate.now() + ".json";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
