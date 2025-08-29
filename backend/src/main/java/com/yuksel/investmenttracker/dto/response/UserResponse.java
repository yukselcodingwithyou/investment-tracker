package com.yuksel.investmenttracker.dto.response;

import com.yuksel.investmenttracker.domain.enums.OAuthProvider;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private String id;
    private String name;
    private String email;
    private boolean emailVerified;
    private List<OAuthProvider> providers;
    private String baseCurrency;
    private String timezone;
    private LocalDateTime createdAt;
}