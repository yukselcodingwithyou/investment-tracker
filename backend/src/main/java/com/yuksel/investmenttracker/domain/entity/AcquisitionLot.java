package com.yuksel.investmenttracker.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "acquisition_lots")
@CompoundIndex(def = "{'userId' : 1, 'assetId': 1, 'acquisitionDate': -1}")
public class AcquisitionLot {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String assetId;
    
    private BigDecimal quantity;
    
    private BigDecimal unitPrice;
    
    private String currency;
    
    private BigDecimal fee;
    
    private LocalDate acquisitionDate;
    
    private BigDecimal fxRateAtAcquisition;
    
    private String notes;
    
    private List<String> tags;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}