package com.yuksel.investmenttracker.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PortfolioSummaryResponse {
    private BigDecimal totalValueTRY;
    private BigDecimal todayChangePercent;
    private BigDecimal totalUnrealizedPLTRY;
    private BigDecimal totalUnrealizedPLPercent;
    private String status; // "UP" or "DOWN"
    
    // If liquidated now section
    private BigDecimal estimatedProceedsTRY;
    private BigDecimal costBasisTRY;
    private BigDecimal unrealizedGainLossTRY;
    private BigDecimal unrealizedGainLossPercent;
    private BigDecimal fxInfluenceTRY;
}