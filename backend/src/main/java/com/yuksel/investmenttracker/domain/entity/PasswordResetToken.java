package com.yuksel.investmenttracker.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    @Indexed(unique = true)
    private String token;
    
    @Indexed
    private LocalDateTime expiresAt;
    
    private boolean used = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime usedAt;
}