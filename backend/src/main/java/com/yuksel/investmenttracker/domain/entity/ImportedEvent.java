package com.yuksel.investmenttracker.domain.entity;

import com.yuksel.investmenttracker.domain.enums.ImportedEventType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "imported_events")
@CompoundIndex(def = "{'userId' : 1, 'assetId': 1, 'date': -1}")
public class ImportedEvent {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed
    private String assetId;
    
    @Indexed
    private ImportedEventType type;
    
    private LocalDate date;
    
    private BigDecimal quantity;
    
    private BigDecimal amount;
    
    private String currency;
    
    private String note;
    
    private LocalDateTime createdAt;
}