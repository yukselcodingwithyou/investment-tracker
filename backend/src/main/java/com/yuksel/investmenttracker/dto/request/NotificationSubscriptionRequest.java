package com.yuksel.investmenttracker.dto.request;

import lombok.Data;

@Data
public class NotificationSubscriptionRequest {
    private String deviceToken;
    private String platform; // "ios", "android", "web"
    private boolean portfolioUpdatesEnabled = true;
    private boolean priceAlertsEnabled = true;
    private boolean weeklyReportsEnabled = true;
    private boolean marketNewsEnabled = false;
}