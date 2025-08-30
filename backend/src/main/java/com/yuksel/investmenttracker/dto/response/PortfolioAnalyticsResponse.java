package com.yuksel.investmenttracker.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PortfolioAnalyticsResponse {
    private List<PortfolioHistoryResponse> portfolioHistory;
    private List<AssetAllocationResponse> assetAllocation;
    private List<TopMoversResponse> topMovers;
    private BigDecimal totalReturn;
    private BigDecimal totalReturnPercent;
    private BigDecimal volatility;
    private BigDecimal sharpeRatio;
    private BigDecimal maxDrawdown;
}