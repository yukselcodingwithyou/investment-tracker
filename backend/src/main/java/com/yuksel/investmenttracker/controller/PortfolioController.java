package com.yuksel.investmenttracker.controller;

import com.yuksel.investmenttracker.domain.entity.AcquisitionLot;
import com.yuksel.investmenttracker.dto.request.AcquisitionRequest;
import com.yuksel.investmenttracker.dto.response.PortfolioSummaryResponse;
import com.yuksel.investmenttracker.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}