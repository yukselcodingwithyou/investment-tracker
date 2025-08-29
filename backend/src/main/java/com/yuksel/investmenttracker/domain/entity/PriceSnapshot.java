package com.yuksel.investmenttracker.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "price_snapshots")
@CompoundIndex(def = "{'assetId' : 1, 'asOf': -1}")
public class PriceSnapshot {
    @Id
    private String id;
    
    @Indexed
    private String assetId;
    
    private BigDecimal price;
    
    private String currency;
    
    @Indexed
    private LocalDateTime asOf;
    
    private String source;
}