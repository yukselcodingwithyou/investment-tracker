package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.NotificationHistory;
import com.yuksel.investmenttracker.domain.entity.NotificationSubscription;
import com.yuksel.investmenttracker.domain.enums.NotificationType;
import com.yuksel.investmenttracker.repository.NotificationHistoryRepository;
import com.yuksel.investmenttracker.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository historyRepository;

    public void subscribeToNotifications(String userId, String deviceToken, String platform) {
        NotificationSubscription subscription = subscriptionRepository
                .findByUserIdAndDeviceToken(userId, deviceToken)
                .orElse(new NotificationSubscription());

        subscription.setUserId(userId);
        subscription.setDeviceToken(deviceToken);
        subscription.setPlatform(platform);
        subscription.setEnabled(true);
        subscription.setUpdatedAt(LocalDateTime.now());
        
        if (subscription.getId() == null) {
            subscription.setCreatedAt(LocalDateTime.now());
        }

        subscriptionRepository.save(subscription);
        log.info("User {} subscribed to notifications on platform {}", userId, platform);
    }

    public void unsubscribeFromNotifications(String userId, String deviceToken) {
        subscriptionRepository.deleteByUserIdAndDeviceToken(userId, deviceToken);
        log.info("User {} unsubscribed from notifications for device {}", userId, deviceToken);
    }

    public void updateNotificationPreferences(String userId, String deviceToken, 
                                            boolean portfolioUpdates, boolean priceAlerts, 
                                            boolean weeklyReports, boolean marketNews) {
        NotificationSubscription subscription = subscriptionRepository
                .findByUserIdAndDeviceToken(userId, deviceToken)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setPortfolioUpdatesEnabled(portfolioUpdates);
        subscription.setPriceAlertsEnabled(priceAlerts);
        subscription.setWeeklyReportsEnabled(weeklyReports);
        subscription.setMarketNewsEnabled(marketNews);
        subscription.setUpdatedAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);
        log.info("Updated notification preferences for user {}", userId);
    }

    public CompletableFuture<Void> sendNotification(String userId, NotificationType type, 
                                                   String title, String body) {
        return CompletableFuture.runAsync(() -> {
            List<NotificationSubscription> subscriptions = 
                    subscriptionRepository.findByUserIdAndEnabled(userId, true);

            for (NotificationSubscription subscription : subscriptions) {
                if (shouldSendNotification(subscription, type)) {
                    sendPushNotification(subscription, title, body, type);
                }
            }
        });
    }

    public void sendBroadcastNotification(NotificationType type, String title, String body) {
        List<NotificationSubscription> allSubscriptions = subscriptionRepository.findByEnabledTrue();
        
        CompletableFuture.runAsync(() -> {
            for (NotificationSubscription subscription : allSubscriptions) {
                if (shouldSendNotification(subscription, type)) {
                    sendPushNotification(subscription, title, body, type);
                }
            }
        });
        
        log.info("Broadcast notification sent to {} subscribers", allSubscriptions.size());
    }

    public void sendPortfolioUpdate(String userId, String message) {
        sendNotification(userId, NotificationType.PORTFOLIO_UPDATE, 
                        "Portfolio Update", message);
    }

    public void sendPriceAlert(String userId, String assetSymbol, String priceInfo) {
        sendNotification(userId, NotificationType.PRICE_ALERT,
                        "Price Alert: " + assetSymbol, priceInfo);
    }

    public void sendWeeklyReport(String userId, String reportSummary) {
        sendNotification(userId, NotificationType.WEEKLY_REPORT,
                        "Weekly Portfolio Report", reportSummary);
    }

    private boolean shouldSendNotification(NotificationSubscription subscription, NotificationType type) {
        return switch (type) {
            case PORTFOLIO_UPDATE -> subscription.isPortfolioUpdatesEnabled();
            case PRICE_ALERT -> subscription.isPriceAlertsEnabled();
            case WEEKLY_REPORT -> subscription.isWeeklyReportsEnabled();
            case MARKET_NEWS -> subscription.isMarketNewsEnabled();
            default -> true;
        };
    }

    private void sendPushNotification(NotificationSubscription subscription, String title, 
                                    String body, NotificationType type) {
        NotificationHistory history = new NotificationHistory();
        history.setUserId(subscription.getUserId());
        history.setDeviceToken(subscription.getDeviceToken());
        history.setTitle(title);
        history.setBody(body);
        history.setType(type.toString());
        history.setCreatedAt(LocalDateTime.now());

        try {
            // Here you would integrate with actual push notification services
            // For now, we'll just log the notification
            boolean success = sendPlatformSpecificNotification(subscription, title, body);
            
            if (success) {
                history.setStatus("SENT");
                history.setSentAt(LocalDateTime.now());
                log.info("Notification sent to user {} on {}: {}", 
                        subscription.getUserId(), subscription.getPlatform(), title);
            } else {
                history.setStatus("FAILED");
                history.setErrorMessage("Platform-specific delivery failed");
                log.warn("Failed to send notification to user {} on {}", 
                        subscription.getUserId(), subscription.getPlatform());
            }
        } catch (Exception e) {
            history.setStatus("FAILED");
            history.setErrorMessage(e.getMessage());
            log.error("Error sending notification to user {}: {}", 
                    subscription.getUserId(), e.getMessage(), e);
        }

        historyRepository.save(history);
    }

    private boolean sendPlatformSpecificNotification(NotificationSubscription subscription, 
                                                   String title, String body) {
        // In a real implementation, this would integrate with:
        // - Firebase Cloud Messaging (FCM) for Android
        // - Apple Push Notification Service (APNs) for iOS
        // - Web Push for web browsers
        
        switch (subscription.getPlatform().toLowerCase()) {
            case "ios":
                return sendAPNSNotification(subscription.getDeviceToken(), title, body);
            case "android":
                return sendFCMNotification(subscription.getDeviceToken(), title, body);
            case "web":
                return sendWebPushNotification(subscription.getDeviceToken(), title, body);
            default:
                log.warn("Unknown platform: {}", subscription.getPlatform());
                return false;
        }
    }

    private boolean sendAPNSNotification(String deviceToken, String title, String body) {
        // TODO: Implement APNS integration
        log.info("Mock APNS notification to {}: {} - {}", deviceToken, title, body);
        return true;
    }

    private boolean sendFCMNotification(String deviceToken, String title, String body) {
        // TODO: Implement FCM integration
        log.info("Mock FCM notification to {}: {} - {}", deviceToken, title, body);
        return true;
    }

    private boolean sendWebPushNotification(String deviceToken, String title, String body) {
        // TODO: Implement Web Push integration
        log.info("Mock Web Push notification to {}: {} - {}", deviceToken, title, body);
        return true;
    }
}