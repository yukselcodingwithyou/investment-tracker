package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.PriceSnapshot;
import com.yuksel.investmenttracker.repository.PriceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final PriceSnapshotRepository priceSnapshotRepository;

    public BigDecimal getCurrentPrice(String assetId, String currency) {
        Optional<PriceSnapshot> latestPrice = priceSnapshotRepository.findLatestByAssetId(assetId);
        
        if (latestPrice.isPresent()) {
            PriceSnapshot priceSnapshot = latestPrice.get();
            
            // For now, return the price as-is. In a real implementation, 
            // currency conversion would be applied here if needed
            log.debug("Found price for asset {}: {} {}", assetId, priceSnapshot.getPrice(), priceSnapshot.getCurrency());
            return priceSnapshot.getPrice();
        } else {
            // If no price found, try to fetch from external API
            log.warn("No price found for asset: {}, attempting to fetch from external source", assetId);
            return fetchAndStorePriceFromExternal(assetId, currency);
        }
    }

    public void updatePriceForAsset(String assetId, BigDecimal price, String currency, String source) {
        PriceSnapshot priceSnapshot = new PriceSnapshot();
        priceSnapshot.setAssetId(assetId);
        priceSnapshot.setPrice(price);
        priceSnapshot.setCurrency(currency);
        priceSnapshot.setAsOf(LocalDateTime.now());
        priceSnapshot.setSource(source);
        
        priceSnapshotRepository.save(priceSnapshot);
        log.info("Updated price for asset {}: {} {}", assetId, price, currency);
    }

    public void updatePricesForAllAssets() {
        // This would be called by a scheduled job to update all asset prices
        // For now, this is a placeholder for batch price updates
        log.info("Batch price update requested - implementation pending for external API integration");
        
        // TODO: Implement batch price updates from external APIs
        // This would involve:
        // 1. Getting all unique asset IDs from acquisitions
        // 2. Fetching prices from external APIs (Yahoo Finance, Alpha Vantage, etc.)
        // 3. Updating price snapshots for each asset
    }

    private BigDecimal fetchAndStorePriceFromExternal(String assetId, String currency) {
        // For now, return a default price since external API integration is not implemented
        // In a real implementation, this would call external price APIs
        log.warn("External price API not implemented, returning default price for asset: {}", assetId);
        
        BigDecimal defaultPrice = BigDecimal.valueOf(100.0); // Default price
        updatePriceForAsset(assetId, defaultPrice, currency, "DEFAULT");
        
        return defaultPrice;
    }
}