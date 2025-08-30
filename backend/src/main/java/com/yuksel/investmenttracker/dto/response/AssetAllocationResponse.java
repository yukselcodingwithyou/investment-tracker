package com.yuksel.investmenttracker.dto.response;

import com.yuksel.investmenttracker.domain.enums.AssetType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetAllocationResponse {
    private AssetType assetType;
    private String assetName;
    private BigDecimal value;
    private BigDecimal percentage;
    private String color; // For chart display
}