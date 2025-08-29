package com.yuksel.investmenttracker.domain.entity;

import com.yuksel.investmenttracker.domain.enums.OAuthProvider;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String name;
    
    @Indexed(unique = true)
    private String email;
    
    private boolean emailVerified;
    
    private String passwordHash;
    
    private List<OAuthProvider> providers;
    
    private String baseCurrency = "TRY";
    
    private String timezone = "Europe/Istanbul";
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}