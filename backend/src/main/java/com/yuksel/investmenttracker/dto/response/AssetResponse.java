package com.yuksel.investmenttracker.dto.response;

import com.yuksel.investmenttracker.domain.enums.AssetType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetResponse {
    private String id;
    private String symbol;
    private String name;
    private AssetType type;
    private String currency;
    
    // Portfolio-specific data
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal currentPrice;
    private BigDecimal dayChangePercent;
    private BigDecimal averageAcquisitionPrice;
    private BigDecimal unrealizedPLTRY;
    private BigDecimal unrealizedPLPercent;
    private BigDecimal marketValueTRY;
}