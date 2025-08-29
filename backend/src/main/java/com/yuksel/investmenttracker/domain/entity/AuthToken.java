package com.yuksel.investmenttracker.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "auth_tokens")
public class AuthToken {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String refreshTokenHash;
    
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;
    
    private String deviceInfo;
    
    private LocalDateTime createdAt;
}