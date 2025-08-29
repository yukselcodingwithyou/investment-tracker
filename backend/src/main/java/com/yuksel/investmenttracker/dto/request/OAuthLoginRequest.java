package com.yuksel.investmenttracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthLoginRequest {
    @NotBlank(message = "Token is required")
    private String token;
    
    private String nonce; // For Apple
}