package com.yuksel.investmenttracker.dto.request;

import com.yuksel.investmenttracker.domain.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class AcquisitionRequest {
    @NotNull(message = "Asset type is required")
    private AssetType assetType;
    
    @NotBlank(message = "Asset symbol is required")
    private String assetSymbol;
    
    private String assetName;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
    
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;
    
    private String currency;
    
    private BigDecimal fee;
    
    @NotNull(message = "Acquisition date is required")
    private LocalDate acquisitionDate;
    
    private String notes;
    
    private List<String> tags;
}