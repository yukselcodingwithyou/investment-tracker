package com.yuksel.investmenttracker.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notification_subscriptions")
public class NotificationSubscription {

    @Id
    private String id;
    
    private String userId;
    private String deviceToken;
    private String platform; // "ios", "android", "web"
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Notification preferences
    private boolean portfolioUpdatesEnabled = true;
    private boolean priceAlertsEnabled = true;
    private boolean weeklyReportsEnabled = true;
    private boolean marketNewsEnabled = false;
}