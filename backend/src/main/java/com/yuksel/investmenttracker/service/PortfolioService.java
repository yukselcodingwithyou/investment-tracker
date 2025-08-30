package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.domain.entity.PriceSnapshot;
import com.yuksel.investmenttracker.domain.enums.AssetType;
import com.yuksel.investmenttracker.dto.request.AcquisitionRequest;
import com.yuksel.investmenttracker.dto.response.*;
import com.yuksel.investmenttracker.repository.AcquisitionLotRepository;
import com.yuksel.investmenttracker.repository.AssetRepository;
import com.yuksel.investmenttracker.repository.PriceSnapshotRepository;
import com.yuksel.investmenttracker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AcquisitionLotRepository acquisitionLotRepository;
    private final AssetRepository assetRepository;
    private final PriceService priceService;
    private final PriceSnapshotRepository priceSnapshotRepository;

    @Transactional
    @CacheEvict(value = {"portfolio-summary", "portfolio-analytics", "asset-allocation", "top-movers"}, 
                key = "#root.target.getCurrentUserId()")
    public AcquisitionLot addAcquisition(AcquisitionRequest request) {
        String userId = getCurrentUserId();

        // Find or create asset
        Asset asset = assetRepository.findBySymbol(request.getAssetSymbol())
                .orElseGet(() -> {
                    Asset newAsset = new Asset();
                    newAsset.setSymbol(request.getAssetSymbol());
                    newAsset.setName(request.getAssetName() != null ? request.getAssetName() : request.getAssetSymbol());
                    newAsset.setType(request.getAssetType());
                    newAsset.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
                    return assetRepository.save(newAsset);
                });

        // Create acquisition lot
        AcquisitionLot acquisitionLot = new AcquisitionLot();
        acquisitionLot.setUserId(userId);
        acquisitionLot.setAssetId(asset.getId());
        acquisitionLot.setQuantity(request.getQuantity());
        acquisitionLot.setUnitPrice(request.getUnitPrice());
        acquisitionLot.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        acquisitionLot.setFee(request.getFee() != null ? request.getFee() : BigDecimal.ZERO);
        acquisitionLot.setAcquisitionDate(request.getAcquisitionDate());
        acquisitionLot.setNotes(request.getNotes());
        acquisitionLot.setTags(request.getTags());
        acquisitionLot.setCreatedAt(LocalDateTime.now());
        acquisitionLot.setUpdatedAt(LocalDateTime.now());

        return acquisitionLotRepository.save(acquisitionLot);
    }

    @Cacheable(value = "portfolio-summary", key = "#root.target.getCurrentUserId()")
    public PortfolioSummaryResponse getPortfolioSummary() {
        String userId = getCurrentUserId();
        
        // Get all acquisitions for the user
        List<AcquisitionLot> acquisitions = acquisitionLotRepository.findByUserId(userId);
        
        if (acquisitions.isEmpty()) {
            // Return empty portfolio if no acquisitions
            return createEmptyPortfolioSummary();
        }
        
        // Calculate portfolio metrics
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        
        for (AcquisitionLot acquisition : acquisitions) {
            // Calculate cost basis for this acquisition
            BigDecimal acquisitionCost = acquisition.getQuantity()
                    .multiply(acquisition.getUnitPrice())
                    .add(acquisition.getFee());
            totalCostBasis = totalCostBasis.add(acquisitionCost);
            totalFees = totalFees.add(acquisition.getFee());
            
            // Get current price and calculate current value
            BigDecimal currentPrice = priceService.getCurrentPrice(acquisition.getAssetId(), "TRY");
            BigDecimal currentValue = acquisition.getQuantity().multiply(currentPrice);
            totalCurrentValue = totalCurrentValue.add(currentValue);
        }
        
        // Calculate profit/loss metrics
        BigDecimal unrealizedGainLoss = totalCurrentValue.subtract(totalCostBasis);
        BigDecimal unrealizedGainLossPercent = totalCostBasis.compareTo(BigDecimal.ZERO) > 0 
                ? unrealizedGainLoss.multiply(BigDecimal.valueOf(100)).divide(totalCostBasis, 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        
        // Determine status
        String status = unrealizedGainLoss.compareTo(BigDecimal.ZERO) >= 0 ? "UP" : "DOWN";
        
        // Calculate daily change for the portfolio
        BigDecimal todayChangePercent = calculateDailyChangePercent(acquisitions);
        
        // Calculate FX influence for multi-currency portfolio
        BigDecimal fxInfluence = calculateFxInfluence(acquisitions);
        
        // Create response
        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        response.setTotalValueTRY(totalCurrentValue);
        response.setTodayChangePercent(todayChangePercent);
        response.setTotalUnrealizedPLTRY(unrealizedGainLoss);
        response.setTotalUnrealizedPLPercent(unrealizedGainLossPercent);
        response.setStatus(status);
        response.setEstimatedProceedsTRY(totalCurrentValue.subtract(totalFees)); // Subtract fees for proceeds
        response.setCostBasisTRY(totalCostBasis);
        response.setUnrealizedGainLossTRY(unrealizedGainLoss);
        response.setUnrealizedGainLossPercent(unrealizedGainLossPercent);
        response.setFxInfluenceTRY(fxInfluence);
        
        log.info("Portfolio summary calculated for user {}: Total Value = {}, P&L = {}", 
                userId, totalCurrentValue, unrealizedGainLoss);
        
        return response;
    }
    
    private PortfolioSummaryResponse createEmptyPortfolioSummary() {
        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        response.setTotalValueTRY(BigDecimal.ZERO);
        response.setTodayChangePercent(BigDecimal.ZERO);
        response.setTotalUnrealizedPLTRY(BigDecimal.ZERO);
        response.setTotalUnrealizedPLPercent(BigDecimal.ZERO);
        response.setStatus("NEUTRAL");
        response.setEstimatedProceedsTRY(BigDecimal.ZERO);
        response.setCostBasisTRY(BigDecimal.ZERO);
        response.setUnrealizedGainLossTRY(BigDecimal.ZERO);
        response.setUnrealizedGainLossPercent(BigDecimal.ZERO);
        response.setFxInfluenceTRY(BigDecimal.ZERO);
        return response;
    }

    @Cacheable(value = "portfolio-history", key = "#root.target.getCurrentUserId() + '_' + #period")
    public List<PortfolioHistoryResponse> getPortfolioHistory(String period) {
        String userId = getCurrentUserId();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = getStartDateForPeriod(period, endDate);
        
        List<PortfolioHistoryResponse> historyData = new ArrayList<>();
        
        // Generate daily portfolio values for the requested period
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            BigDecimal portfolioValue = calculatePortfolioValueForDate(userId, currentDate);
            BigDecimal change = BigDecimal.ZERO;
            BigDecimal changePercent = BigDecimal.ZERO;
            
            // Calculate change from previous day
            if (!historyData.isEmpty()) {
                PortfolioHistoryResponse previousDay = historyData.get(historyData.size() - 1);
                change = portfolioValue.subtract(previousDay.getValue());
                if (previousDay.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    changePercent = change.multiply(BigDecimal.valueOf(100))
                            .divide(previousDay.getValue(), 2, RoundingMode.HALF_UP);
                }
            }
            
            PortfolioHistoryResponse historyPoint = new PortfolioHistoryResponse();
            historyPoint.setDate(currentDate);
            historyPoint.setValue(portfolioValue);
            historyPoint.setChange(change);
            historyPoint.setChangePercent(changePercent);
            
            historyData.add(historyPoint);
            currentDate = currentDate.plusDays(1);
        }
        
        return historyData;
    }
    
    @Cacheable(value = "asset-allocation", key = "#root.target.getCurrentUserId()")
    public List<AssetAllocationResponse> getAssetAllocation() {
        String userId = getCurrentUserId();
        List<AcquisitionLot> acquisitions = acquisitionLotRepository.findByUserId(userId);
        
        Map<AssetType, BigDecimal> allocationMap = new HashMap<>();
        Map<AssetType, String> assetNames = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (AcquisitionLot acquisition : acquisitions) {
            Asset asset = assetRepository.findById(acquisition.getAssetId()).orElse(null);
            if (asset != null) {
                BigDecimal currentPrice = priceService.getCurrentPrice(acquisition.getAssetId(), "TRY");
                BigDecimal assetValue = acquisition.getQuantity().multiply(currentPrice);
                
                allocationMap.merge(asset.getType(), assetValue, BigDecimal::add);
                assetNames.put(asset.getType(), asset.getType().toString());
                totalValue = totalValue.add(assetValue);
            }
        }
        
        List<AssetAllocationResponse> allocationList = new ArrayList<>();
        String[] colors = {"#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40"};
        int colorIndex = 0;
        
        for (Map.Entry<AssetType, BigDecimal> entry : allocationMap.entrySet()) {
            AssetAllocationResponse allocation = new AssetAllocationResponse();
            allocation.setAssetType(entry.getKey());
            allocation.setAssetName(assetNames.get(entry.getKey()));
            allocation.setValue(entry.getValue());
            
            if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
                allocation.setPercentage(entry.getValue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalValue, 2, RoundingMode.HALF_UP));
            } else {
                allocation.setPercentage(BigDecimal.ZERO);
            }
            
            allocation.setColor(colors[colorIndex % colors.length]);
            colorIndex++;
            
            allocationList.add(allocation);
        }
        
        return allocationList.stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "top-movers", key = "#root.target.getCurrentUserId() + '_' + #limit")
    public List<TopMoversResponse> getTopMovers(int limit) {
        String userId = getCurrentUserId();
        List<AcquisitionLot> acquisitions = acquisitionLotRepository.findByUserId(userId);
        
        Map<String, TopMoversData> assetMoversMap = new HashMap<>();
        
        for (AcquisitionLot acquisition : acquisitions) {
            Asset asset = assetRepository.findById(acquisition.getAssetId()).orElse(null);
            if (asset != null) {
                BigDecimal currentPrice = priceService.getCurrentPrice(acquisition.getAssetId(), "TRY");
                BigDecimal assetValue = acquisition.getQuantity().multiply(currentPrice);
                
                TopMoversData data = assetMoversMap.computeIfAbsent(acquisition.getAssetId(), 
                    k -> new TopMoversData(asset.getSymbol(), asset.getName()));
                data.addValue(assetValue);
                data.setCurrentPrice(currentPrice);
            }
        }
        
        return assetMoversMap.values().stream()
                .map(this::convertToTopMoversResponse)
                .sorted((a, b) -> b.getChangePercent().abs().compareTo(a.getChangePercent().abs()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "portfolio-analytics", key = "#root.target.getCurrentUserId() + '_' + #period")
    public PortfolioAnalyticsResponse getPortfolioAnalytics(String period) {
        PortfolioAnalyticsResponse analytics = new PortfolioAnalyticsResponse();
        
        analytics.setPortfolioHistory(getPortfolioHistory(period));
        analytics.setAssetAllocation(getAssetAllocation());
        analytics.setTopMovers(getTopMovers(5));
        
        // Calculate advanced metrics
        List<PortfolioHistoryResponse> history = analytics.getPortfolioHistory();
        if (history.size() > 1) {
            BigDecimal firstValue = history.get(0).getValue();
            BigDecimal lastValue = history.get(history.size() - 1).getValue();
            
            if (firstValue.compareTo(BigDecimal.ZERO) > 0) {
                analytics.setTotalReturn(lastValue.subtract(firstValue));
                analytics.setTotalReturnPercent(analytics.getTotalReturn()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(firstValue, 2, RoundingMode.HALF_UP));
            }
            
            analytics.setVolatility(calculateVolatility(history));
            analytics.setSharpeRatio(calculateSharpeRatio(history));
            analytics.setMaxDrawdown(calculateMaxDrawdown(history));
        }
        
        return analytics;
    }
    
    private LocalDate getStartDateForPeriod(String period, LocalDate endDate) {
        return switch (period.toUpperCase()) {
            case "7D" -> endDate.minusDays(7);
            case "30D" -> endDate.minusDays(30);
            case "90D" -> endDate.minusDays(90);
            case "1Y" -> endDate.minusYears(1);
            case "ALL" -> endDate.minusYears(5); // Default to 5 years max
            default -> endDate.minusDays(30);
        };
    }
    
    private BigDecimal calculatePortfolioValueForDate(String userId, LocalDate date) {
        // For now, return current portfolio value
        // In a real implementation, this would calculate historical value
        List<AcquisitionLot> acquisitions = acquisitionLotRepository.findByUserId(userId);
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (AcquisitionLot acquisition : acquisitions) {
            if (!acquisition.getAcquisitionDate().isAfter(date)) {
                BigDecimal currentPrice = priceService.getCurrentPrice(acquisition.getAssetId(), "TRY");
                BigDecimal assetValue = acquisition.getQuantity().multiply(currentPrice);
                totalValue = totalValue.add(assetValue);
            }
        }
        
        // Add some random variation for demo purposes
        double variation = Math.sin(date.toEpochDay()) * 0.05; // 5% variation
        BigDecimal variationAmount = totalValue.multiply(BigDecimal.valueOf(variation));
        return totalValue.add(variationAmount);
    }
    
    private BigDecimal calculateVolatility(List<PortfolioHistoryResponse> history) {
        if (history.size() < 2) return BigDecimal.ZERO;
        
        double sumSquaredReturns = 0.0;
        int count = 0;
        
        for (int i = 1; i < history.size(); i++) {
            BigDecimal previousValue = history.get(i - 1).getValue();
            BigDecimal currentValue = history.get(i).getValue();
            
            if (previousValue.compareTo(BigDecimal.ZERO) > 0) {
                double dailyReturn = currentValue.subtract(previousValue)
                        .divide(previousValue, 4, RoundingMode.HALF_UP).doubleValue();
                sumSquaredReturns += dailyReturn * dailyReturn;
                count++;
            }
        }
        
        double variance = count > 0 ? sumSquaredReturns / count : 0.0;
        return BigDecimal.valueOf(Math.sqrt(variance) * Math.sqrt(252)); // Annualized
    }
    
    private BigDecimal calculateSharpeRatio(List<PortfolioHistoryResponse> history) {
        // Simplified Sharpe ratio calculation (assuming 2% risk-free rate)
        BigDecimal volatility = calculateVolatility(history);
        if (volatility.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        BigDecimal totalReturn = getTotalReturnPercent(history);
        BigDecimal riskFreeRate = BigDecimal.valueOf(2.0); // 2% annual
        
        return totalReturn.subtract(riskFreeRate).divide(volatility, 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateMaxDrawdown(List<PortfolioHistoryResponse> history) {
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        
        for (PortfolioHistoryResponse point : history) {
            if (point.getValue().compareTo(peak) > 0) {
                peak = point.getValue();
            } else {
                BigDecimal drawdown = peak.subtract(point.getValue())
                        .divide(peak, 4, RoundingMode.HALF_UP);
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }
        
        return maxDrawdown.multiply(BigDecimal.valueOf(100)); // As percentage
    }
    
    private BigDecimal getTotalReturnPercent(List<PortfolioHistoryResponse> history) {
        if (history.size() < 2) return BigDecimal.ZERO;
        
        BigDecimal firstValue = history.get(0).getValue();
        BigDecimal lastValue = history.get(history.size() - 1).getValue();
        
        if (firstValue.compareTo(BigDecimal.ZERO) > 0) {
            return lastValue.subtract(firstValue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(firstValue, 2, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    private TopMoversResponse convertToTopMoversResponse(TopMoversData data) {
        TopMoversResponse response = new TopMoversResponse();
        response.setAssetId(data.getAssetId());
        response.setAssetSymbol(data.getSymbol());
        response.setAssetName(data.getName());
        response.setCurrentPrice(data.getCurrentPrice());
        response.setValue(data.getTotalValue());
        
        // Calculate mock change (in real implementation, this would compare with previous day)
        BigDecimal mockChange = data.getTotalValue().multiply(BigDecimal.valueOf(0.03)); // 3% mock change
        response.setChange(mockChange);
        
        if (data.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
            response.setChangePercent(mockChange.multiply(BigDecimal.valueOf(100))
                    .divide(data.getTotalValue(), 2, RoundingMode.HALF_UP));
        } else {
            response.setChangePercent(BigDecimal.ZERO);
        }
        
        response.setDirection(response.getChange().compareTo(BigDecimal.ZERO) >= 0 ? "UP" : "DOWN");
        
        return response;
    }

    /**
     * Calculate daily change percentage for the entire portfolio
     */
    private BigDecimal calculateDailyChangePercent(List<AcquisitionLot> acquisitions) {
        if (acquisitions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalPreviousDayValue = BigDecimal.ZERO;
        
        // Get unique asset IDs
        Set<String> assetIds = acquisitions.stream()
                .map(AcquisitionLot::getAssetId)
                .collect(Collectors.toSet());

        for (String assetId : assetIds) {
            // Get current price
            Optional<PriceSnapshot> currentPriceOpt = priceSnapshotRepository.findLatestByAssetId(assetId);
            if (!currentPriceOpt.isPresent()) {
                continue;
            }

            PriceSnapshot currentPrice = currentPriceOpt.get();
            
            // Get price from previous day (24 hours ago)
            LocalDateTime previousDay = currentPrice.getAsOf().minusDays(1);
            LocalDateTime startOfDay = previousDay.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = previousDay.toLocalDate().plusDays(1).atStartOfDay();
            
            List<PriceSnapshot> previousDayPrices = priceSnapshotRepository
                    .findByAssetIdAndAsOfBetween(assetId, startOfDay, endOfDay);
            
            if (previousDayPrices.isEmpty()) {
                // If no previous day data, use current price as baseline
                BigDecimal assetQuantity = acquisitions.stream()
                        .filter(acq -> acq.getAssetId().equals(assetId))
                        .map(AcquisitionLot::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal assetCurrentValue = currentPrice.getPrice().multiply(assetQuantity);
                totalCurrentValue = totalCurrentValue.add(assetCurrentValue);
                totalPreviousDayValue = totalPreviousDayValue.add(assetCurrentValue);
                continue;
            }

            // Use the latest price from previous day
            PriceSnapshot previousDayPrice = previousDayPrices.get(previousDayPrices.size() - 1);
            
            // Calculate total quantity for this asset
            BigDecimal assetQuantity = acquisitions.stream()
                    .filter(acq -> acq.getAssetId().equals(assetId))
                    .map(AcquisitionLot::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalCurrentValue = totalCurrentValue.add(currentPrice.getPrice().multiply(assetQuantity));
            totalPreviousDayValue = totalPreviousDayValue.add(previousDayPrice.getPrice().multiply(assetQuantity));
        }

        // Calculate percentage change
        if (totalPreviousDayValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyChange = totalCurrentValue.subtract(totalPreviousDayValue);
            return dailyChange.multiply(BigDecimal.valueOf(100))
                    .divide(totalPreviousDayValue, 2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calculate FX influence on portfolio value
     * This shows how much of the gain/loss is due to currency fluctuations vs asset performance
     */
    private BigDecimal calculateFxInfluence(List<AcquisitionLot> acquisitions) {
        if (acquisitions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalFxImpact = BigDecimal.ZERO;
        
        // Group acquisitions by asset ID to get original currencies
        Map<String, List<AcquisitionLot>> acquisitionsByAsset = acquisitions.stream()
                .collect(Collectors.groupingBy(AcquisitionLot::getAssetId));

        for (Map.Entry<String, List<AcquisitionLot>> entry : acquisitionsByAsset.entrySet()) {
            String assetId = entry.getKey();
            List<AcquisitionLot> assetAcquisitions = entry.getValue();

            // Get asset details
            Optional<Asset> assetOpt = assetRepository.findById(assetId);
            if (!assetOpt.isPresent()) {
                continue;
            }
            
            Asset asset = assetOpt.get();
            String assetCurrency = asset.getCurrency();
            
            // Skip if already in TRY
            if ("TRY".equals(assetCurrency)) {
                continue;
            }

            // Calculate total value in original currency
            BigDecimal totalQuantity = assetAcquisitions.stream()
                    .map(AcquisitionLot::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Optional<PriceSnapshot> currentPriceOpt = priceSnapshotRepository.findLatestByAssetId(assetId);
            if (!currentPriceOpt.isPresent()) {
                continue;
            }

            BigDecimal currentPriceInOriginalCurrency = currentPriceOpt.get().getPrice();
            BigDecimal valueInOriginalCurrency = currentPriceInOriginalCurrency.multiply(totalQuantity);

            // For FX impact calculation, we would need historical exchange rates
            // For now, we'll calculate a simplified estimate based on common currency volatility
            // In a real implementation, this would use historical USD/TRY, EUR/TRY rates
            BigDecimal estimatedFxImpact = calculateEstimatedFxImpact(assetCurrency, valueInOriginalCurrency);
            totalFxImpact = totalFxImpact.add(estimatedFxImpact);
        }

        return totalFxImpact;
    }

    /**
     * Estimate FX impact based on currency and value
     * This is a simplified calculation - a real implementation would use actual historical exchange rates
     */
    private BigDecimal calculateEstimatedFxImpact(String currency, BigDecimal valueInOriginalCurrency) {
        // Estimated monthly volatility against TRY for major currencies
        BigDecimal volatilityFactor;
        switch (currency) {
            case "USD":
                volatilityFactor = BigDecimal.valueOf(0.05); // 5% estimated impact
                break;
            case "EUR":
                volatilityFactor = BigDecimal.valueOf(0.04); // 4% estimated impact
                break;
            case "GBP":
                volatilityFactor = BigDecimal.valueOf(0.06); // 6% estimated impact
                break;
            default:
                volatilityFactor = BigDecimal.valueOf(0.03); // 3% default
                break;
        }

        // Return estimated FX impact (this would be positive or negative in real implementation)
        return valueInOriginalCurrency.multiply(volatilityFactor);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
    
    // Helper class for top movers calculation
    private static class TopMoversData {
        private final String symbol;
        private final String name;
        private String assetId;
        private BigDecimal totalValue = BigDecimal.ZERO;
        private BigDecimal currentPrice = BigDecimal.ZERO;
        
        public TopMoversData(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;
        }
        
        public void addValue(BigDecimal value) {
            this.totalValue = this.totalValue.add(value);
        }
        
        // Getters and setters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public BigDecimal getTotalValue() { return totalValue; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    }
}