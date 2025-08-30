package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.dto.response.AssetResponse;
import com.yuksel.investmenttracker.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    public Page<AssetResponse> searchAssets(String search, String type, String currency, Pageable pageable) {
        Page<Asset> assets;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search by symbol or name
            assets = assetRepository.findBySymbolOrNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            // Get all assets with optional filters
            assets = assetRepository.findAll(pageable);
        }
        
        // Apply additional filters if provided
        List<Asset> filteredAssets = assets.getContent().stream()
                .filter(asset -> type == null || type.equals(asset.getType().toString()))
                .filter(asset -> currency == null || currency.equals(asset.getCurrency()))
                .collect(Collectors.toList());
        
        List<AssetResponse> assetResponses = filteredAssets.stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(assetResponses, pageable, assets.getTotalElements());
    }

    public Page<AssetResponse> searchAssetsByQuery(String query, Pageable pageable) {
        Page<Asset> assets = assetRepository.findBySymbolOrNameContainingIgnoreCase(query, pageable);
        
        List<AssetResponse> assetResponses = assets.getContent().stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(assetResponses, pageable, assets.getTotalElements());
    }

    public AssetResponse getAssetById(String id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));
        
        return mapToAssetResponse(asset);
    }

    public Asset findOrCreateAsset(String symbol, String name, String type, String currency) {
        return assetRepository.findBySymbol(symbol)
                .orElseGet(() -> {
                    Asset newAsset = new Asset();
                    newAsset.setSymbol(symbol);
                    newAsset.setName(name != null ? name : symbol);
                    newAsset.setType(com.yuksel.investmenttracker.domain.enums.AssetType.valueOf(type));
                    newAsset.setCurrency(currency != null ? currency : "USD");
                    return assetRepository.save(newAsset);
                });
    }

    public Asset getAssetEntityById(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
        AssetResponse response = new AssetResponse();
        response.setId(asset.getId());
        response.setSymbol(asset.getSymbol());
        response.setName(asset.getName());
        response.setType(asset.getType());
        response.setCurrency(asset.getCurrency());
        // Portfolio-specific fields would be set elsewhere when needed
        return response;
    }
}