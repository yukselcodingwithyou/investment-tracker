package com.yuksel.investmenttracker.dto;

import lombok.Data;

@Data
public class OAuthUserInfo {
    private String email;
    private String name;
    private String providerId;
    private boolean emailVerified;
}