package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.domain.entity.Asset;
import com.yuksel.investmenttracker.dto.request.AcquisitionRequest;
import com.yuksel.investmenttracker.dto.response.PortfolioSummaryResponse;
import com.yuksel.investmenttracker.repository.AcquisitionLotRepository;
import com.yuksel.investmenttracker.repository.AssetRepository;
import com.yuksel.investmenttracker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AcquisitionLotRepository acquisitionLotRepository;
    private final AssetRepository assetRepository;
    private final PriceService priceService;

    @Transactional
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
        
        // Create response
        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        response.setTotalValueTRY(totalCurrentValue);
        response.setTodayChangePercent(BigDecimal.ZERO); // TODO: Calculate daily change
        response.setTotalUnrealizedPLTRY(unrealizedGainLoss);
        response.setTotalUnrealizedPLPercent(unrealizedGainLossPercent);
        response.setStatus(status);
        response.setEstimatedProceedsTRY(totalCurrentValue.subtract(totalFees)); // Subtract fees for proceeds
        response.setCostBasisTRY(totalCostBasis);
        response.setUnrealizedGainLossTRY(unrealizedGainLoss);
        response.setUnrealizedGainLossPercent(unrealizedGainLossPercent);
        response.setFxInfluenceTRY(BigDecimal.ZERO); // TODO: Calculate FX influence
        
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

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
}