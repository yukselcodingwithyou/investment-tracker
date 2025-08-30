package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.repository.AcquisitionLotRepository;
import com.yuksel.investmenttracker.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimePriceUpdateService {

    private final PriceService priceService;
    private final AssetRepository assetRepository;
    private final AcquisitionLotRepository acquisitionLotRepository;
    private final NotificationService notificationService;
    private final Random random = new Random();

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Async
    public void updateAllAssetPrices() {
        log.info("Starting scheduled price update at {}", LocalDateTime.now());
        
        try {
            // Get all unique assets that are held in portfolios
            Set<String> activeAssetIds = acquisitionLotRepository.findAll()
                    .stream()
                    .map(AcquisitionLot::getAssetId)
                    .collect(Collectors.toSet());

            log.info("Found {} active assets to update", activeAssetIds.size());

            List<CompletableFuture<Void>> updateFutures = activeAssetIds.stream()
                    .map(this::updateAssetPriceAsync)
                    .toList();

            // Wait for all price updates to complete
            CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        log.info("Completed price update for {} assets", activeAssetIds.size());
                        
                        // Send notifications for significant price changes
                        checkForSignificantPriceChanges();
                    });

        } catch (Exception e) {
            log.error("Error during scheduled price update", e);
        }
    }

    @Async
    public CompletableFuture<Void> updateAssetPriceAsync(String assetId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Asset asset = assetRepository.findById(assetId).orElse(null);
                if (asset == null) {
                    log.warn("Asset not found for ID: {}", assetId);
                    return;
                }

                // Simulate price fetching from external API
                BigDecimal newPrice = fetchPriceFromExternalAPI(asset);
                
                if (newPrice != null) {
                    priceService.updatePriceForAsset(assetId, newPrice, "TRY", "REAL_TIME_UPDATE");
                    log.debug("Updated price for {} ({}): {}", asset.getSymbol(), assetId, newPrice);
                } else {
                    log.warn("Failed to fetch price for asset: {} ({})", asset.getSymbol(), assetId);
                }

            } catch (Exception e) {
                log.error("Error updating price for asset: {}", assetId, e);
            }
        });
    }

    @Async
    public CompletableFuture<Void> updateSingleAssetPrice(String assetId) {
        log.info("Manual price update requested for asset: {}", assetId);
        return updateAssetPriceAsync(assetId);
    }

    private BigDecimal fetchPriceFromExternalAPI(Asset asset) {
        // In a real implementation, this would integrate with external APIs:
        // - Yahoo Finance API
        // - Alpha Vantage
        // - IEX Cloud
        // - Polygon.io
        // - Binance API (for crypto)
        // - CBRT (for FX rates from Turkish Central Bank)
        
        try {
            // Simulate API call delay
            Thread.sleep(100 + random.nextInt(200)); // 100-300ms delay
            
            // Generate mock price data based on asset type
            return generateMockPrice(asset);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while fetching price for {}", asset.getSymbol());
            return null;
        } catch (Exception e) {
            log.error("Error fetching price for {}: {}", asset.getSymbol(), e.getMessage());
            return null;
        }
    }

    private BigDecimal generateMockPrice(Asset asset) {
        // Get base price based on asset type
        BigDecimal basePrice = switch (asset.getType()) {
            case EQUITY -> BigDecimal.valueOf(100 + random.nextInt(400)); // $100-500
            case FX -> BigDecimal.valueOf(25 + random.nextDouble() * 10); // 25-35 TRY
            case PRECIOUS_METAL -> BigDecimal.valueOf(2000 + random.nextInt(1000)); // $2000-3000
            case FUND -> BigDecimal.valueOf(50 + random.nextInt(100)); // $50-150
        };

        // Add some realistic price movement (-2% to +2%)
        double changePercent = (random.nextDouble() - 0.5) * 0.04; // -2% to +2%
        BigDecimal priceChange = basePrice.multiply(BigDecimal.valueOf(changePercent));
        
        return basePrice.add(priceChange).max(BigDecimal.valueOf(0.01)); // Minimum price of 0.01
    }

    private void checkForSignificantPriceChanges() {
        // This would check for significant price movements (>5%) and send notifications
        log.info("Checking for significant price changes to notify users");
        
        // In a real implementation, this would:
        // 1. Compare current prices with previous prices
        // 2. Calculate percentage changes
        // 3. Identify assets with significant movements
        // 4. Send targeted notifications to users holding those assets
        
        // For demonstration, send a general market update
        notificationService.sendBroadcastNotification(
                com.yuksel.investmenttracker.domain.enums.NotificationType.MARKET_NEWS,
                "Market Update",
                "Prices have been updated for your portfolio assets."
        );
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI") // Weekdays at 9 AM
    public void sendDailyMarketOpen() {
        log.info("Sending daily market open notifications");
        
        notificationService.sendBroadcastNotification(
                com.yuksel.investmenttracker.domain.enums.NotificationType.MARKET_NEWS,
                "Markets Open",
                "Markets are now open. Check your portfolio for overnight changes."
        );
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI") // Weekdays at 6 PM
    public void sendDailyMarketClose() {
        log.info("Sending daily market close notifications");
        
        notificationService.sendBroadcastNotification(
                com.yuksel.investmenttracker.domain.enums.NotificationType.MARKET_NEWS,
                "Markets Closed",
                "Markets have closed. Review your portfolio performance for today."
        );
    }

    public void forceUpdateAllPrices() {
        log.info("Force updating all asset prices");
        updateAllAssetPrices();
    }
}