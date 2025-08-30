package com.yuksel.investmenttracker.controller;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.dto.request.AcquisitionRequest;
import com.yuksel.investmenttracker.dto.response.*;
import com.yuksel.investmenttracker.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfolio management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("/acquisitions")
    @Operation(summary = "Add new acquisition")
    public ResponseEntity<AcquisitionLot> addAcquisition(@Valid @RequestBody AcquisitionRequest request) {
        AcquisitionLot acquisition = portfolioService.addAcquisition(request);
        return ResponseEntity.ok(acquisition);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary")
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary() {
        PortfolioSummaryResponse summary = portfolioService.getPortfolioSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/history")
    @Operation(summary = "Get portfolio value history")
    public ResponseEntity<List<PortfolioHistoryResponse>> getPortfolioHistory(
            @RequestParam(defaultValue = "30D") String period) {
        List<PortfolioHistoryResponse> history = portfolioService.getPortfolioHistory(period);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/allocation")
    @Operation(summary = "Get asset allocation breakdown")
    public ResponseEntity<List<AssetAllocationResponse>> getAssetAllocation() {
        List<AssetAllocationResponse> allocation = portfolioService.getAssetAllocation();
        return ResponseEntity.ok(allocation);
    }

    @GetMapping("/top-movers")
    @Operation(summary = "Get top performing assets")
    public ResponseEntity<List<TopMoversResponse>> getTopMovers(
            @RequestParam(defaultValue = "5") int limit) {
        List<TopMoversResponse> topMovers = portfolioService.getTopMovers(limit);
        return ResponseEntity.ok(topMovers);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get comprehensive portfolio analytics")
    public ResponseEntity<PortfolioAnalyticsResponse> getPortfolioAnalytics(
            @RequestParam(defaultValue = "30D") String period) {
        PortfolioAnalyticsResponse analytics = portfolioService.getPortfolioAnalytics(period);
        return ResponseEntity.ok(analytics);
    }
}