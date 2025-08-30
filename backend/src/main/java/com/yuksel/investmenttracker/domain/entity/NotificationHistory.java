package com.yuksel.investmenttracker.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notification_history")
public class NotificationHistory {

    @Id
    private String id;
    
    private String userId;
    private String deviceToken;
    private String title;
    private String body;
    private String type; // "PORTFOLIO_UPDATE", "PRICE_ALERT", "WEEKLY_REPORT", etc.
    private String status; // "SENT", "FAILED", "PENDING"
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}