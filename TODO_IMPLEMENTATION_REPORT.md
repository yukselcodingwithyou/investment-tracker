# Investment Tracker - Remaining TODOs and Implementation Guide

This document provides a comprehensive guide for implementing the remaining TODOs that require external dependencies, API integrations, or significant architectural changes.

## 🎯 Implementation Status Summary

### ✅ COMPLETED TODOs

#### Backend Implementations
- ✅ Portfolio daily change calculation (PortfolioService.java:114)
- ✅ FX influence calculation (PortfolioService.java:122)
- ✅ Apple OAuth token validation structure (OAuthService.java:65)
- ✅ Price service batch update structure (PriceService.java:61)
- ✅ Currency service external API structure (CurrencyService.java:104)
- ✅ Email service enhancements (welcome, portfolio summary, account deletion emails)

#### Android Implementations
- ✅ Refresh token logic with authentication expiration flow (TokenManager.kt:52)
- ✅ Settings screen actions with SettingsViewModel (SettingsScreen.kt - 8 TODOs)
- ✅ Dashboard navigation improvements (DashboardScreen.kt:54)
- ✅ History screen import functionality enhancements (HistoryScreen.kt:39)
- ✅ OAuth placeholder implementations with detailed guidance (AuthScreen.kt)
- ✅ Portfolio Repository with data flow structure
- ✅ Asset Repository with mock data and search functionality
- ✅ Import/Export Service with CSV format support

#### iOS Implementations
- ✅ Basic structure exists (TODOs identified in analysis but not directly modified)

---

## 🔧 REMAINING TODOs - External Dependencies Required

### 1. 🔐 OAuth Integration (High Priority)

#### Google Sign-In Setup
**Files to modify:**
- `android-app/app/build.gradle` - Add Google Sign-In SDK
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/auth/AuthScreen.kt`
- `ios-app/InvestmentTracker/Services/OAuthService.swift`

**Implementation Steps:**
```gradle
// Add to android-app/app/build.gradle
implementation 'com.google.android.gms:play-services-auth:20.7.0'
```

**Configuration Required:**
1. Create Google Cloud Console project
2. Configure OAuth 2.0 credentials
3. Add SHA-1 fingerprints for Android
4. Configure authorized domains
5. Set up client IDs for iOS and Android

**API Integration:**
- Backend: `OAuthService.java` - validateGoogleToken() method needs actual verification
- Android: Replace placeholder in AuthScreen with GoogleSignInClient
- iOS: Implement Sign in with Google SDK

#### Apple Sign-In Setup
**Implementation Steps:**
1. Apple Developer Account setup
2. Configure App ID with Sign In with Apple capability
3. Create Service ID for web authentication
4. Configure domain verification
5. Generate private key for JWT signing

**Backend Changes:**
- `OAuthService.java` - Complete Apple token signature verification
- Add Apple's public key fetching and caching
- Implement JWT signature validation

---

### 2. 💰 External Price API Integration (High Priority)

#### Recommended Services:
1. **Alpha Vantage** (Free tier: 5 API requests per minute)
2. **Yahoo Finance API** (Unofficial but reliable)
3. **IEX Cloud** (Freemium model)
4. **Finnhub** (Free tier: 60 API calls/minute)

#### Implementation:
**Backend Files:**
- `PriceService.java` - Replace mock implementation in updateSingleAssetPrice()
- `CurrencyService.java` - Replace mock implementation in fetchExchangeRateFromAPI()

**Example Integration (Alpha Vantage):**
```java
// Add to PriceService.java
private BigDecimal fetchPriceFromAlphaVantage(String symbol) {
    String apiKey = "YOUR_API_KEY";
    String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
    
    // HTTP client implementation
    // JSON parsing
    // Error handling
}
```

**Configuration:**
1. Register for API key
2. Add rate limiting
3. Implement caching strategy
4. Handle API errors gracefully

---

### 3. 📧 Email Service Provider Setup (Medium Priority)

#### Recommended Providers:
1. **SendGrid** (Free tier: 100 emails/day)
2. **AWS SES** (Pay-as-you-go)
3. **Mailgun** (Free tier: 1,000 emails/month)

#### Configuration:
**Backend - application.yml:**
```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: ${SENDGRID_API_KEY}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Environment Variables:**
- `SENDGRID_API_KEY`
- `EMAIL_FROM_ADDRESS`
- `EMAIL_FROM_NAME`

---

### 4. 📱 Push Notifications (Medium Priority)

#### Backend Setup:
**Files:**
- `NotificationService.java` - Implement APNS, FCM, Web Push

**Implementation Required:**
1. **Firebase Cloud Messaging (FCM)** for Android
2. **Apple Push Notification Service (APNS)** for iOS
3. **Web Push** for future web app

**Configuration:**
```java
// NotificationService.java
public void sendFcmNotification(String deviceToken, String title, String body) {
    // Firebase Admin SDK implementation
}

public void sendApnsNotification(String deviceToken, String title, String body) {
    // APNS implementation
}
```

---

### 5. 📊 Advanced Portfolio Features (Medium Priority)

#### Missing Backend APIs:
1. **Portfolio History Endpoint**
   ```java
   @GetMapping("/portfolio/history")
   public List<PortfolioHistoryPoint> getPortfolioHistory(@RequestParam String period)
   ```

2. **Asset Allocation Endpoint**
   ```java
   @GetMapping("/portfolio/allocation")
   public List<AssetAllocationPoint> getAssetAllocation()
   ```

3. **Top Movers Enhancement**
   - Real-time price comparison
   - Market hours detection
   - Extended market data

#### Android Integration:
- Update `PortfolioRepository.kt` to use real APIs
- Implement chart data visualization
- Add real-time updates

---

### 6. 📁 File Import/Export (Low Priority)

#### Android Implementation:
**Files:**
- `ImportExportService.kt` - Replace mock implementations

**Required Dependencies:**
```gradle
// Add to android-app/app/build.gradle
implementation 'org.apache.commons:commons-csv:1.9.0'
implementation 'androidx.activity:activity-compose:1.8.0' // For file picker
```

**Implementation:**
1. File picker integration
2. CSV parsing with Apache Commons CSV
3. Data validation and preview
4. Background processing for large files
5. Progress indicators

#### Backend Support:
1. Bulk import endpoint
2. Data validation
3. Error reporting
4. Transaction management

---

### 7. 🗄️ Database Optimization (Low Priority)

#### Required Indexes:
```javascript
// MongoDB indexes
db.users.createIndex({ email: 1 }, { unique: true });
db.acquisition_lots.createIndex({ userId: 1, assetId: 1 });
db.price_snapshots.createIndex({ assetId: 1, asOf: -1 });
db.auth_tokens.createIndex({ userId: 1 });
db.auth_tokens.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 });
```

#### Backend Configuration:
- Connection pooling optimization
- Query optimization
- Caching strategy implementation

---

## 🚀 Deployment Considerations

### Security Requirements:
1. **API Keys Management**
   - Use environment variables
   - Implement key rotation
   - Monitor usage limits

2. **SSL/TLS Configuration**
   - Enforce HTTPS
   - Configure proper certificates
   - Implement HSTS headers

3. **Rate Limiting**
   - API endpoint protection
   - External service rate limiting
   - User-based throttling

### Monitoring and Logging:
1. **Application Monitoring**
   - Error tracking (Sentry, Rollbar)
   - Performance monitoring (New Relic, DataDog)
   - Uptime monitoring

2. **Logging Strategy**
   - Structured logging
   - Log aggregation
   - Security event logging

---

## 📋 Implementation Priority

### Phase 1 (Essential for MVP):
1. ✅ Backend portfolio calculations (COMPLETED)
2. ✅ Android UI improvements (COMPLETED)
3. 🔧 Google OAuth integration
4. 🔧 Basic price API integration

### Phase 2 (Enhanced Features):
1. Apple Sign-In
2. Email service provider
3. Advanced portfolio features
4. Push notifications

### Phase 3 (Advanced Features):
1. File import/export
2. Advanced analytics
3. Performance optimizations
4. Advanced monitoring

---

## 🔗 Useful Resources

### API Documentation:
- [Alpha Vantage API](https://www.alphavantage.co/documentation/)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Apple Sign In](https://developer.apple.com/sign-in-with-apple/)
- [SendGrid API](https://docs.sendgrid.com/)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)

### Libraries and SDKs:
- [Google Play Services Auth](https://developers.google.com/android/guides/setup)
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)
- [OkHttp](https://square.github.io/okhttp/)
- [Retrofit](https://square.github.io/retrofit/)

This document serves as a comprehensive guide for completing the Investment Tracker application. All structural TODOs have been implemented, and this report provides clear paths for implementing the remaining features that require external integrations.