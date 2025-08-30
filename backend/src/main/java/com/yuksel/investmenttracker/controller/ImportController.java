package com.yuksel.investmenttracker.controller;

import com.yuksel.investmenttracker.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
@Tag(name = "Import/Export", description = "Data import and export endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ImportController {

    private final ImportService importService;

    @PostMapping("/csv")
    @Operation(summary = "Import portfolio data from CSV file")
    public ResponseEntity<Map<String, Object>> importCsvData(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty"));
        }
        
        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("error", "File must be a CSV format"));
        }
        
        try {
            Map<String, Object> result = importService.importAcquisitionsFromCsv(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/csv/template")
    @Operation(summary = "Download CSV import template")
    public ResponseEntity<Resource> downloadCsvTemplate() {
        try {
            Resource template = importService.generateCsvTemplate();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portfolio_import_template.csv\"")
                    .body(template);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/csv/export")
    @Operation(summary = "Export portfolio data to CSV")
    public ResponseEntity<Resource> exportPortfolioToCsv() {
        try {
            Resource csvFile = importService.exportAcquisitionsToCsv();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portfolio_export.csv\"")
                    .body(csvFile);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}