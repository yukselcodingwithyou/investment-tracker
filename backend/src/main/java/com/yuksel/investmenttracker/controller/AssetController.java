package com.yuksel.investmenttracker.controller;

import com.yuksel.investmenttracker.dto.response.AssetResponse;
import com.yuksel.investmenttracker.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Asset management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @Operation(summary = "List assets with search and filter options")
    public ResponseEntity<Page<AssetResponse>> listAssets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AssetResponse> assets = assetService.searchAssets(search, type, currency, pageable);
        
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset details by ID")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable String id) {
        AssetResponse asset = assetService.getAssetById(id);
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/search")
    @Operation(summary = "Search assets by symbol or name")
    public ResponseEntity<Page<AssetResponse>> searchAssets(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AssetResponse> assets = assetService.searchAssetsByQuery(query, pageable);
        
        return ResponseEntity.ok(assets);
    }
}