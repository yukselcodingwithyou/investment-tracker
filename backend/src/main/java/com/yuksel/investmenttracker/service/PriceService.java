package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.PriceSnapshot;
import com.yuksel.investmenttracker.repository.PriceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final PriceSnapshotRepository priceSnapshotRepository;

    @Cacheable(value = "current-prices", key = "#assetId + '_' + #currency", 
               cacheManager = "pricesCacheManager")
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

    @CacheEvict(value = "current-prices", key = "#assetId + '_*'", 
                cacheManager = "pricesCacheManager")
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
        log.info("Starting batch price update for all assets");
        
        try {
            // Get all unique asset IDs from acquisitions
            List<String> assetIds = getAllActiveAssetIds();
            
            if (assetIds.isEmpty()) {
                log.info("No assets found for price updates");
                return;
            }
            
            log.info("Found {} assets for price updates", assetIds.size());
            
            // Update prices for each asset
            int successCount = 0;
            int failureCount = 0;
            
            for (String assetId : assetIds) {
                try {
                    updateSingleAssetPrice(assetId);
                    successCount++;
                    
                    // Add small delay to avoid rate limiting
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("Failed to update price for asset {}: {}", assetId, e.getMessage());
                    failureCount++;
                }
            }
            
            log.info("Batch price update completed. Success: {}, Failures: {}", successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Batch price update failed", e);
        }
    }
    
    private List<String> getAllActiveAssetIds() {
        // In a real implementation, this would query the acquisition lots to get unique asset IDs
        // For now, return empty list since we don't have external API integration
        log.info("Getting active asset IDs from acquisitions");
        return List.of(); // Placeholder - would query AcquisitionLotRepository
    }
    
    private void updateSingleAssetPrice(String assetId) {
        // This would fetch price from external API and update the price snapshot
        log.info("Updating price for asset: {}", assetId);
        
        // In a real implementation, this would:
        // 1. Fetch current price from external API (Alpha Vantage, Yahoo Finance, etc.)
        // 2. Handle rate limiting and error responses
        // 3. Update price snapshot with new data
        // 4. Cache the result appropriately
        
        // For now, we'll just log that we would update it
        BigDecimal mockPrice = BigDecimal.valueOf(100.0 + Math.random() * 50); // Mock price
        updatePriceForAsset(assetId, mockPrice, "USD", "EXTERNAL_API");
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