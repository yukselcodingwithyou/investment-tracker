package com.yuksel.investmenttracker.domain.entity;

import com.yuksel.investmenttracker.domain.enums.AssetType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "assets")
public class Asset {
    @Id
    private String id;
    
    @Indexed
    private String symbol;
    
    private String name;
    
    @Indexed
    private AssetType type;
    
    private String currency;
    
    private String description;
}