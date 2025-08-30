# Backend Completion Prompt

## 🎯 Objective
Complete all missing backend implementations for the Investment Tracker application to achieve production readiness.

## 🔧 Required Implementations

### 1. OAuth Authentication Service Implementation

**Task:** Implement OAuth token validation for Google and Apple Sign-In

**Files to modify:**
- `backend/src/main/java/com/yuksel/investmenttracker/service/AuthService.java`

**Requirements:**
```java
@Transactional
public AuthResponse oauthLogin(OAuthLoginRequest oauthRequest, OAuthProvider provider) {
    // Validate OAuth token with provider
    OAuthUserInfo userInfo = validateOAuthToken(oauthRequest.getToken(), provider);
    
    // Find or create user
    User user = userRepository.findByEmail(userInfo.getEmail())
        .orElseGet(() -> createUserFromOAuth(userInfo, provider));
    
    // Update providers list if not already present
    if (!user.getProviders().contains(provider)) {
        user.getProviders().add(provider);
        userRepository.save(user);
    }
    
    // Generate tokens and return auth response
    return createAuthResponse(createAuthentication(user));
}

private OAuthUserInfo validateOAuthToken(String token, OAuthProvider provider) {
    // Implementation for Google/Apple token validation
}

private User createUserFromOAuth(OAuthUserInfo userInfo, OAuthProvider provider) {
    // Create user from OAuth info
}
```

**Dependencies to add:**
- Google OAuth2 client library
- Apple ID token validation library
- HTTP client for OAuth provider APIs

### 2. Password Reset Functionality

**Task:** Implement forgot password and reset password functionality

**Files to modify:**
- `backend/src/main/java/com/yuksel/investmenttracker/service/AuthService.java`
- `backend/src/main/java/com/yuksel/investmenttracker/controller/AuthController.java`

**New entities to create:**
```java
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;
    private String userId;
    private String token;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;
}
```

**Service methods to implement:**
```java
public void sendPasswordResetEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException("User not found"));
    
    String resetToken = generateSecureToken();
    PasswordResetToken tokenEntity = new PasswordResetToken();
    tokenEntity.setUserId(user.getId());
    tokenEntity.setToken(resetToken);
    tokenEntity.setExpiresAt(LocalDateTime.now().plusHours(1));
    
    passwordResetTokenRepository.save(tokenEntity);
    emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
}

public void resetPassword(String token, String newPassword) {
    PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
        .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));
    
    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new TokenExpiredException("Reset token has expired");
    }
    
    User user = userRepository.findById(resetToken.getUserId())
        .orElseThrow(() -> new UserNotFoundException("User not found"));
    
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    
    resetToken.setUsed(true);
    passwordResetTokenRepository.save(resetToken);
}
```

### 3. Real Portfolio Calculation Implementation

**Task:** Replace mock portfolio data with real calculations

**Files to modify:**
- `backend/src/main/java/com/yuksel/investmenttracker/service/PortfolioService.java`

**New service to create:**
```java
@Service
@RequiredArgsConstructor
public class PriceService {
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ExternalPriceApiClient externalPriceApiClient;
    
    public BigDecimal getCurrentPrice(String assetId, String currency) {
        return priceSnapshotRepository.findLatestByAssetId(assetId)
            .map(PriceSnapshot::getPrice)
            .orElseGet(() -> fetchAndStorePriceFromExternal(assetId, currency));
    }
    
    public void updatePricesForAllAssets() {
        // Batch price update logic
    }
}
```

**Portfolio calculation implementation:**
```java
public PortfolioSummaryResponse getPortfolioSummary() {
    String userId = getCurrentUserId();
    
    List<AcquisitionLot> acquisitions = acquisitionLotRepository.findByUserId(userId);
    
    BigDecimal totalCostBasis = BigDecimal.ZERO;
    BigDecimal totalCurrentValue = BigDecimal.ZERO;
    
    for (AcquisitionLot acquisition : acquisitions) {
        BigDecimal costBasis = acquisition.getUnitPrice().multiply(acquisition.getQuantity());
        totalCostBasis = totalCostBasis.add(costBasis);
        
        BigDecimal currentPrice = priceService.getCurrentPrice(acquisition.getAssetId(), "TRY");
        BigDecimal currentValue = currentPrice.multiply(acquisition.getQuantity());
        totalCurrentValue = totalCurrentValue.add(currentValue);
    }
    
    BigDecimal unrealizedPL = totalCurrentValue.subtract(totalCostBasis);
    BigDecimal unrealizedPLPercent = unrealizedPL.divide(totalCostBasis, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
    
    PortfolioSummaryResponse response = new PortfolioSummaryResponse();
    response.setTotalValueTRY(totalCurrentValue);
    response.setCostBasisTRY(totalCostBasis);
    response.setUnrealizedGainLossTRY(unrealizedPL);
    response.setUnrealizedGainLossPercent(unrealizedPLPercent);
    response.setStatus(unrealizedPL.compareTo(BigDecimal.ZERO) >= 0 ? "UP" : "DOWN");
    
    return response;
}
```

### 4. Asset Management Controller

**Files to create:**
- `backend/src/main/java/com/yuksel/investmenttracker/controller/AssetController.java`
- `backend/src/main/java/com/yuksel/investmenttracker/service/AssetService.java`

**Controller implementation:**
```java
@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Asset management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {
    
    private final AssetService assetService;
    
    @GetMapping
    @Operation(summary = "List assets with search and filter")
    public ResponseEntity<PagedResponse<AssetResponse>> getAssets(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) AssetType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PagedResponse<AssetResponse> assets = assetService.searchAssets(search, type, page, size);
        return ResponseEntity.ok(assets);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get asset details")
    public ResponseEntity<AssetDetailResponse> getAssetDetails(@PathVariable String id) {
        AssetDetailResponse asset = assetService.getAssetDetails(id);
        return ResponseEntity.ok(asset);
    }
    
    @PostMapping
    @Operation(summary = "Create new asset")
    public ResponseEntity<Asset> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        Asset asset = assetService.createAsset(request);
        return ResponseEntity.status(201).body(asset);
    }
}
```

### 5. CSV Import/Export Controller

**Files to create:**
- `backend/src/main/java/com/yuksel/investmenttracker/controller/ImportController.java`
- `backend/src/main/java/com/yuksel/investmenttracker/service/ImportService.java`

**Controller implementation:**
```java
@RestController
@RequestMapping("/imports")
@RequiredArgsConstructor
@Tag(name = "Import/Export", description = "Data import and export endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ImportController {
    
    private final ImportService importService;
    
    @PostMapping("/csv")
    @Operation(summary = "Import portfolio data from CSV")
    public ResponseEntity<ImportResultResponse> importFromCsv(
            @RequestParam("file") MultipartFile file) {
        
        ImportResultResponse result = importService.importFromCsv(file);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/csv/template")
    @Operation(summary = "Download CSV template")
    public ResponseEntity<Resource> downloadCsvTemplate() {
        Resource template = importService.generateCsvTemplate();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=portfolio_template.csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(template);
    }
    
    @GetMapping("/export/csv")
    @Operation(summary = "Export portfolio data to CSV")
    public ResponseEntity<Resource> exportToCsv() {
        Resource csvFile = importService.exportToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=portfolio_export.csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(csvFile);
    }
}
```

### 6. Email Service Implementation

**Files to create:**
- `backend/src/main/java/com/yuksel/investmenttracker/service/EmailService.java`

**Implementation:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from}")
    private String fromEmail;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset - Investment Tracker");
            
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            String htmlContent = buildPasswordResetEmailContent(resetUrl);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
    
    private String buildPasswordResetEmailContent(String resetUrl) {
        return """
            <html>
            <body>
                <h2>Password Reset Request</h2>
                <p>You have requested to reset your password for Investment Tracker.</p>
                <p>Click the link below to reset your password:</p>
                <a href="%s">Reset Password</a>
                <p>This link will expire in 1 hour.</p>
                <p>If you did not request this reset, please ignore this email.</p>
            </body>
            </html>
            """.formatted(resetUrl);
    }
}
```

### 7. External Price API Integration

**Files to create:**
- `backend/src/main/java/com/yuksel/investmenttracker/integration/ExternalPriceApiClient.java`

**Implementation:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalPriceApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.price-api.url}")
    private String priceApiUrl;
    
    @Value("${external.price-api.key}")
    private String apiKey;
    
    public BigDecimal fetchPrice(String symbol, String currency) {
        try {
            String url = priceApiUrl + "/quote?symbol=" + symbol + "&currency=" + currency;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<PriceApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, PriceApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getPrice();
            }
            
            throw new PriceDataException("Failed to fetch price for " + symbol);
            
        } catch (Exception e) {
            log.error("Error fetching price for symbol: {}", symbol, e);
            throw new PriceDataException("Price fetch failed", e);
        }
    }
    
    public Map<String, BigDecimal> fetchBatchPrices(List<String> symbols, String currency) {
        // Batch price fetching implementation
    }
}
```

### 8. Scheduled Price Updates

**Files to create:**
- `backend/src/main/java/com/yuksel/investmenttracker/scheduler/PriceUpdateScheduler.java`

**Implementation:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class PriceUpdateScheduler {
    
    private final PriceService priceService;
    private final AssetRepository assetRepository;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updatePrices() {
        log.info("Starting scheduled price update");
        
        try {
            List<Asset> assets = assetRepository.findAll();
            for (Asset asset : assets) {
                priceService.updatePrice(asset);
            }
            log.info("Completed price update for {} assets", assets.size());
            
        } catch (Exception e) {
            log.error("Error during scheduled price update", e);
        }
    }
    
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 9 AM on weekdays
    public void dailyPriceUpdate() {
        log.info("Starting daily comprehensive price update");
        priceService.updatePricesForAllAssets();
    }
}
```

## 🔧 Dependencies to Add

**Add to `backend/build.gradle`:**
```gradle
dependencies {
    // OAuth2 support
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'com.google.api-client:google-api-client:1.34.1'
    
    // Email support
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    
    // CSV processing
    implementation 'com.opencsv:opencsv:5.7.1'
    
    // Scheduling
    implementation 'org.springframework.boot:spring-boot-starter-quartz'
    
    // HTTP client
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

## ⚙️ Configuration

**Add to `application.yml`:**
```yaml
app:
  mail:
    from: noreply@investmenttracker.com
  frontend:
    url: ${FRONTEND_URL:http://localhost:3000}

external:
  price-api:
    url: ${PRICE_API_URL:https://api.example.com/v1}
    key: ${PRICE_API_KEY:your-api-key}

spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope: openid,profile,email
          apple:
            client-id: ${APPLE_CLIENT_ID:}
            client-secret: ${APPLE_CLIENT_SECRET:}
```

## 🧪 Testing Requirements

**Create test files:**
- `AuthServiceTest.java` - OAuth and password reset testing
- `PortfolioServiceTest.java` - Portfolio calculation testing
- `AssetControllerTest.java` - Asset API testing
- `ImportServiceTest.java` - CSV import/export testing
- `PriceServiceTest.java` - Price fetching testing

## 📦 Deployment Checklist

- [ ] Configure OAuth provider credentials
- [ ] Set up email service credentials
- [ ] Configure external price API access
- [ ] Set up database indexes for performance
- [ ] Configure rate limiting for APIs
- [ ] Set up monitoring and logging
- [ ] Configure CORS for frontend domains
- [ ] Set up SSL certificates
- [ ] Configure environment-specific properties

## 🎯 Success Criteria

After implementing these changes:
- [ ] All TODO comments resolved
- [ ] OAuth login functional for Google and Apple
- [ ] Password reset flow working end-to-end
- [ ] Portfolio calculations use real price data
- [ ] Asset management APIs fully functional
- [ ] CSV import/export working
- [ ] Comprehensive test coverage (>80%)
- [ ] Production-ready configuration
- [ ] API documentation complete
- [ ] Performance optimized with proper indexing