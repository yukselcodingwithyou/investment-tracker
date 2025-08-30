package com.yuksel.investmenttracker.controller;

import com.yuksel.investmenttracker.dto.request.NotificationSubscriptionRequest;
import com.yuksel.investmenttracker.dto.response.NotificationSubscriptionResponse;
import com.yuksel.investmenttracker.service.NotificationService;
import com.yuksel.investmenttracker.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Push notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to push notifications")
    public ResponseEntity<String> subscribe(@RequestBody NotificationSubscriptionRequest request,
                                          Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        
        notificationService.subscribeToNotifications(
                userId, 
                request.getDeviceToken(), 
                request.getPlatform()
        );
        
        // Update preferences if provided
        notificationService.updateNotificationPreferences(
                userId,
                request.getDeviceToken(),
                request.isPortfolioUpdatesEnabled(),
                request.isPriceAlertsEnabled(),
                request.isWeeklyReportsEnabled(),
                request.isMarketNewsEnabled()
        );
        
        return ResponseEntity.ok("Successfully subscribed to notifications");
    }

    @PostMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe from push notifications")
    public ResponseEntity<String> unsubscribe(@RequestParam String deviceToken,
                                            Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        notificationService.unsubscribeFromNotifications(userId, deviceToken);
        return ResponseEntity.ok("Successfully unsubscribed from notifications");
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<String> updatePreferences(@RequestBody NotificationSubscriptionRequest request,
                                                   Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        
        notificationService.updateNotificationPreferences(
                userId,
                request.getDeviceToken(),
                request.isPortfolioUpdatesEnabled(),
                request.isPriceAlertsEnabled(),
                request.isWeeklyReportsEnabled(),
                request.isMarketNewsEnabled()
        );
        
        return ResponseEntity.ok("Notification preferences updated successfully");
    }

    @PostMapping("/test")
    @Operation(summary = "Send test notification")
    public ResponseEntity<String> sendTestNotification(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        
        notificationService.sendPortfolioUpdate(userId, 
                "This is a test notification from Investment Tracker!");
        
        return ResponseEntity.ok("Test notification sent");
    }

    private String getCurrentUserId(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
}