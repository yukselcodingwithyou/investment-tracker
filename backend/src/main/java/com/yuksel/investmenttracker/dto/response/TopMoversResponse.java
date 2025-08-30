package com.yuksel.investmenttracker.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopMoversResponse {
    private String assetId;
    private String assetSymbol;
    private String assetName;
    private BigDecimal currentPrice;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal value;
    private String direction; // "UP" or "DOWN"
}