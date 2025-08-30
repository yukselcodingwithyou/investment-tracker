package com.yuksel.investmenttracker.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationSubscriptionResponse {
    private String id;
    private String deviceToken;
    private String platform;
    private boolean enabled;
    private boolean portfolioUpdatesEnabled;
    private boolean priceAlertsEnabled;
    private boolean weeklyReportsEnabled;
    private boolean marketNewsEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}