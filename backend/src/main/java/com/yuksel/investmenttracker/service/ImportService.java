package com.yuksel.investmenttracker.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.domain.enums.AssetType;
import com.yuksel.investmenttracker.dto.request.AcquisitionRequest;
import com.yuksel.investmenttracker.repository.AcquisitionLotRepository;
import com.yuksel.investmenttracker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final PortfolioService portfolioService;
    private final AssetService assetService;
    private final AcquisitionLotRepository acquisitionLotRepository;

    private static final String[] CSV_HEADER = {
            "Symbol", "Name", "Asset Type", "Quantity", "Unit Price", 
            "Currency", "Fee", "Acquisition Date", "Notes", "Tags"
    };

    @Transactional
    public Map<String, Object> importAcquisitionsFromCsv(MultipartFile file) throws IOException {
        String userId = getCurrentUserId();
        List<String> errors = new ArrayList<>();
        List<AcquisitionLot> importedAcquisitions = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            
            if (headers == null || !validateHeaders(headers)) {
                throw new IllegalArgumentException("Invalid CSV format. Please use the provided template.");
            }

            String[] line;
            int lineNumber = 2; // Starting from line 2 (after header)
            
            while ((line = reader.readNext()) != null) {
                try {
                    if (isEmptyLine(line)) {
                        continue;
                    }
                    
                    AcquisitionRequest request = parseLineToAcquisitionRequest(line, lineNumber);
                    AcquisitionLot acquisition = portfolioService.addAcquisition(request);
                    importedAcquisitions.add(acquisition);
                    successCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    log.warn("Error processing line {}: {}", lineNumber, e.getMessage());
                }
                lineNumber++;
            }
            
        } catch (CsvValidationException e) {
            throw new IllegalArgumentException("CSV parsing error: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        result.put("totalProcessed", successCount + errorCount);
        
        log.info("CSV import completed for user {}: {} successful, {} errors", userId, successCount, errorCount);
        
        return result;
    }

    public Resource generateCsvTemplate() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            // Write headers
            writer.writeNext(CSV_HEADER);
            
            // Write sample data
            String[] sampleRow = {
                    "AAPL", "Apple Inc.", "EQUITY", "10", "150.50", 
                    "USD", "9.99", "2023-01-15", "Sample acquisition", "tech,portfolio"
            };
            writer.writeNext(sampleRow);
        }
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    public Resource exportAcquisitionsToCsv() throws IOException {
        String userId = getCurrentUserId();
        List<AcquisitionLot> acquisitions = acquisitionLotRepository.findByUserId(userId);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            // Write headers
            writer.writeNext(CSV_HEADER);
            
            // Write data
            for (AcquisitionLot acquisition : acquisitions) {
                Asset asset = assetService.getAssetEntityById(acquisition.getAssetId());
                
                String[] row = {
                        asset.getSymbol(),
                        asset.getName(),
                        asset.getType().toString(),
                        acquisition.getQuantity().toString(),
                        acquisition.getUnitPrice().toString(),
                        acquisition.getCurrency(),
                        acquisition.getFee().toString(),
                        acquisition.getAcquisitionDate().toString(),
                        acquisition.getNotes() != null ? acquisition.getNotes() : "",
                        acquisition.getTags() != null ? String.join(",", acquisition.getTags()) : ""
                };
                writer.writeNext(row);
            }
        }
        
        log.info("Exported {} acquisitions to CSV for user {}", acquisitions.size(), userId);
        
        return new ByteArrayResource(outputStream.toByteArray());
    }

    private boolean validateHeaders(String[] headers) {
        if (headers.length < CSV_HEADER.length) {
            return false;
        }
        
        for (int i = 0; i < CSV_HEADER.length; i++) {
            if (!CSV_HEADER[i].equalsIgnoreCase(headers[i].trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean isEmptyLine(String[] line) {
        for (String cell : line) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private AcquisitionRequest parseLineToAcquisitionRequest(String[] line, int lineNumber) {
        try {
            AcquisitionRequest request = new AcquisitionRequest();
            
            // Symbol (required)
            if (line.length > 0 && line[0] != null && !line[0].trim().isEmpty()) {
                request.setAssetSymbol(line[0].trim().toUpperCase());
            } else {
                throw new IllegalArgumentException("Symbol is required");
            }
            
            // Name (optional)
            if (line.length > 1 && line[1] != null && !line[1].trim().isEmpty()) {
                request.setAssetName(line[1].trim());
            }
            
            // Asset Type (required)
            if (line.length > 2 && line[2] != null && !line[2].trim().isEmpty()) {
                try {
                    request.setAssetType(AssetType.valueOf(line[2].trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid asset type: " + line[2]);
                }
            } else {
                request.setAssetType(AssetType.EQUITY); // Default
            }
            
            // Quantity (required)
            if (line.length > 3 && line[3] != null && !line[3].trim().isEmpty()) {
                try {
                    request.setQuantity(new BigDecimal(line[3].trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid quantity: " + line[3]);
                }
            } else {
                throw new IllegalArgumentException("Quantity is required");
            }
            
            // Unit Price (required)
            if (line.length > 4 && line[4] != null && !line[4].trim().isEmpty()) {
                try {
                    request.setUnitPrice(new BigDecimal(line[4].trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid unit price: " + line[4]);
                }
            } else {
                throw new IllegalArgumentException("Unit price is required");
            }
            
            // Currency (optional, default to USD)
            if (line.length > 5 && line[5] != null && !line[5].trim().isEmpty()) {
                request.setCurrency(line[5].trim().toUpperCase());
            } else {
                request.setCurrency("USD");
            }
            
            // Fee (optional, default to 0)
            if (line.length > 6 && line[6] != null && !line[6].trim().isEmpty()) {
                try {
                    request.setFee(new BigDecimal(line[6].trim()));
                } catch (NumberFormatException e) {
                    request.setFee(BigDecimal.ZERO);
                }
            } else {
                request.setFee(BigDecimal.ZERO);
            }
            
            // Acquisition Date (required)
            if (line.length > 7 && line[7] != null && !line[7].trim().isEmpty()) {
                try {
                    request.setAcquisitionDate(LocalDate.parse(line[7].trim(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid date format (use YYYY-MM-DD): " + line[7]);
                }
            } else {
                request.setAcquisitionDate(LocalDate.now());
            }
            
            // Notes (optional)
            if (line.length > 8 && line[8] != null && !line[8].trim().isEmpty()) {
                request.setNotes(line[8].trim());
            }
            
            // Tags (optional)
            if (line.length > 9 && line[9] != null && !line[9].trim().isEmpty()) {
                String[] tags = line[9].split(",");
                List<String> tagList = new ArrayList<>();
                for (String tag : tags) {
                    tagList.add(tag.trim());
                }
                request.setTags(tagList);
            }
            
            return request;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing line: " + e.getMessage());
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
}