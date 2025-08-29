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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AcquisitionLotRepository acquisitionLotRepository;
    private final AssetRepository assetRepository;

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
        
        // TODO: Implement portfolio calculation with real price data
        // For now, return a sample response
        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        response.setTotalValueTRY(BigDecimal.valueOf(100000));
        response.setTodayChangePercent(BigDecimal.valueOf(2.5));
        response.setTotalUnrealizedPLTRY(BigDecimal.valueOf(5000));
        response.setTotalUnrealizedPLPercent(BigDecimal.valueOf(5.26));
        response.setStatus("UP");
        response.setEstimatedProceedsTRY(BigDecimal.valueOf(95000));
        response.setCostBasisTRY(BigDecimal.valueOf(90000));
        response.setUnrealizedGainLossTRY(BigDecimal.valueOf(5000));
        response.setUnrealizedGainLossPercent(BigDecimal.valueOf(5.56));
        response.setFxInfluenceTRY(BigDecimal.valueOf(1000));
        
        return response;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
}