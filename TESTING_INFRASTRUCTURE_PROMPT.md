# Testing Infrastructure Completion Prompt

## 🎯 Objective
Establish comprehensive testing infrastructure across Backend, iOS, and Android platforms for the Investment Tracker application.

## 🧪 Backend Testing Implementation

### 1. Test Dependencies and Configuration

**Add to `backend/build.gradle`:**
```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mongodb'
    testImplementation 'org.mockito:mockito-inline:4.11.0'
    testImplementation 'com.github.tomakehurst:wiremock-jre8:2.35.0'
    testImplementation 'org.springframework.cloud:spring-cloud-starter-contract-stub-runner'
}
```

### 2. Unit Tests Implementation

**Files to create:**
- `backend/src/test/java/com/yuksel/investmenttracker/service/AuthServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuthTokenRepository authTokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider tokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    void signUp_Success() {
        // Given
        SignUpRequest signUpRequest = SignUpRequest.builder()
            .name("John Doe")
            .email("john@example.com")
            .password("password123")
            .confirmPassword("password123")
            .build();
        
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        
        User savedUser = createTestUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        
        AuthResponse expectedResponse = createTestAuthResponse();
        when(tokenProvider.generateToken(any())).thenReturn("test_token");
        
        // When
        AuthResponse response = authService.signUp(signUpRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test_token");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void signUp_EmailAlreadyExists_ThrowsException() {
        // Given
        SignUpRequest signUpRequest = SignUpRequest.builder()
            .email("existing@example.com")
            .build();
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> authService.signUp(signUpRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Email address already in use");
    }
    
    @Test
    void oauthLogin_GoogleProvider_Success() throws Exception {
        // Given
        OAuthLoginRequest request = new OAuthLoginRequest("valid_token", "GOOGLE");
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
            .email("google@example.com")
            .name("Google User")
            .build();
        
        when(oauthValidator.validateGoogleToken("valid_token")).thenReturn(userInfo);
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.empty());
        
        User savedUser = createTestUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // When
        AuthResponse response = authService.oauthLogin(request, OAuthProvider.GOOGLE);
        
        // Then
        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void forgotPassword_ValidEmail_SendsResetEmail() {
        // Given
        String email = "user@example.com";
        User user = createTestUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        
        // When
        authService.sendPasswordResetEmail(email);
        
        // Then
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(email), anyString());
    }
    
    private User createTestUser() {
        User user = new User();
        user.setId("user123");
        user.setEmail("test@example.com");
        user.setName("Test User");
        return user;
    }
    
    private AuthResponse createTestAuthResponse() {
        return AuthResponse.builder()
            .accessToken("test_token")
            .refreshToken("refresh_token")
            .user(UserResponse.builder()
                .id("user123")
                .email("test@example.com")
                .name("Test User")
                .build())
            .build();
    }
}
```

**Portfolio Service Tests:**
```java
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    
    @Mock
    private AcquisitionLotRepository acquisitionLotRepository;
    
    @Mock
    private AssetRepository assetRepository;
    
    @Mock
    private PriceService priceService;
    
    @InjectMocks
    private PortfolioService portfolioService;
    
    @Test
    void getPortfolioSummary_WithAcquisitions_CalculatesCorrectly() {
        // Given
        String userId = "user123";
        List<AcquisitionLot> acquisitions = createTestAcquisitions();
        
        when(acquisitionLotRepository.findByUserId(userId)).thenReturn(acquisitions);
        when(priceService.getCurrentPrice("asset1", "TRY")).thenReturn(BigDecimal.valueOf(110));
        when(priceService.getCurrentPrice("asset2", "TRY")).thenReturn(BigDecimal.valueOf(55));
        
        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockSecurityContext(userId, mockedSecurity);
            
            // When
            PortfolioSummaryResponse response = portfolioService.getPortfolioSummary();
            
            // Then
            assertThat(response.getTotalValueTRY()).isEqualTo(BigDecimal.valueOf(1650)); // (110*10) + (55*5)
            assertThat(response.getCostBasisTRY()).isEqualTo(BigDecimal.valueOf(1500)); // (100*10) + (50*5)
            assertThat(response.getUnrealizedGainLossTRY()).isEqualTo(BigDecimal.valueOf(150)); // 1650-1500
            assertThat(response.getStatus()).isEqualTo("UP");
        }
    }
    
    @Test
    void addAcquisition_ValidRequest_SavesSuccessfully() {
        // Given
        AcquisitionRequest request = AcquisitionRequest.builder()
            .assetId("asset123")
            .quantity(BigDecimal.valueOf(100))
            .unitPrice(BigDecimal.valueOf(50))
            .currency("USD")
            .acquisitionDate(LocalDate.now())
            .build();
        
        Asset asset = createTestAsset();
        when(assetRepository.findById("asset123")).thenReturn(Optional.of(asset));
        
        AcquisitionLot savedLot = createTestAcquisitionLot();
        when(acquisitionLotRepository.save(any(AcquisitionLot.class))).thenReturn(savedLot);
        
        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockSecurityContext("user123", mockedSecurity);
            
            // When
            AcquisitionLot result = portfolioService.addAcquisition(request);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAssetId()).isEqualTo("asset123");
            verify(acquisitionLotRepository).save(any(AcquisitionLot.class));
        }
    }
    
    private List<AcquisitionLot> createTestAcquisitions() {
        return Arrays.asList(
            AcquisitionLot.builder()
                .assetId("asset1")
                .quantity(BigDecimal.valueOf(10))
                .unitPrice(BigDecimal.valueOf(100))
                .build(),
            AcquisitionLot.builder()
                .assetId("asset2")
                .quantity(BigDecimal.valueOf(5))
                .unitPrice(BigDecimal.valueOf(50))
                .build()
        );
    }
}
```

### 3. Integration Tests

**Files to create:**
- `backend/src/test/java/com/yuksel/investmenttracker/controller/AuthControllerIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4.3"));
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void signUp_ValidRequest_ReturnsSuccessResponse() {
        // Given
        SignUpRequest request = SignUpRequest.builder()
            .name("John Doe")
            .email("john@example.com")
            .password("password123")
            .confirmPassword("password123")
            .build();
        
        // When
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/auth/signup", request, AuthResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        
        // Verify user is saved in database
        Optional<User> savedUser = userRepository.findByEmail("john@example.com");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("John Doe");
    }
    
    @Test
    void login_ValidCredentials_ReturnsAuthResponse() {
        // Given - create user first
        createTestUser("test@example.com", "password123");
        
        LoginRequest loginRequest = LoginRequest.builder()
            .emailOrUsername("test@example.com")
            .password("password123")
            .build();
        
        // When
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/auth/login", loginRequest, AuthResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isNotBlank();
    }
    
    @Test
    void getCurrentUser_WithValidToken_ReturnsUserInfo() {
        // Given
        String accessToken = createUserAndGetToken("authenticated@example.com");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/auth/me", HttpMethod.GET, entity, UserResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("authenticated@example.com");
    }
    
    private void createTestUser(String email, String password) {
        SignUpRequest request = SignUpRequest.builder()
            .name("Test User")
            .email(email)
            .password(password)
            .confirmPassword(password)
            .build();
        
        restTemplate.postForEntity("/auth/signup", request, AuthResponse.class);
    }
    
    private String createUserAndGetToken(String email) {
        createTestUser(email, "password123");
        
        LoginRequest loginRequest = LoginRequest.builder()
            .emailOrUsername(email)
            .password("password123")
            .build();
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/auth/login", loginRequest, AuthResponse.class);
        
        return response.getBody().getAccessToken();
    }
}
```

### 4. Security Tests

**Files to create:**
- `backend/src/test/java/com/yuksel/investmenttracker/security/JwtTokenProviderTest.java`

```java
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    
    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    void generateToken_ValidAuthentication_ReturnsValidToken() {
        // Given
        Authentication authentication = createTestAuthentication();
        
        // When
        String token = jwtTokenProvider.generateToken(authentication);
        
        // Then
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }
    
    @Test
    void getUserIdFromToken_ValidToken_ReturnsCorrectUserId() {
        // Given
        Authentication authentication = createTestAuthentication();
        String token = jwtTokenProvider.generateToken(authentication);
        
        // When
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        
        // Then
        assertThat(userId).isEqualTo("user123");
    }
    
    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Given
        String expiredToken = createExpiredToken();
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    private Authentication createTestAuthentication() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
            .id("user123")
            .email("test@example.com")
            .build();
        
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, Collections.emptyList());
    }
}
```

## 📱 iOS Testing Implementation

### 1. Test Dependencies and Configuration

**Add to iOS project target:**
- XCTest framework
- Testing bundle identifier

### 2. Unit Tests

**Files to create:**
- `ios-app/InvestmentTrackerTests/Services/APIClientTests.swift`

```swift
import XCTest
@testable import InvestmentTracker

class APIClientTests: XCTestCase {
    
    var apiClient: APIClient!
    var mockKeychainService: MockKeychainService!
    var mockURLSession: MockURLSession!
    
    override func setUp() {
        super.setUp()
        mockKeychainService = MockKeychainService()
        mockURLSession = MockURLSession()
        apiClient = APIClient()
        // Inject mocks
    }
    
    override func tearDown() {
        apiClient = nil
        mockKeychainService = nil
        mockURLSession = nil
        super.tearDown()
    }
    
    func testRequest_SuccessfulResponse_ReturnsDecodedData() async throws {
        // Given
        let expectedUser = User(id: "123", name: "Test User", email: "test@example.com")
        let jsonData = try JSONEncoder().encode(expectedUser)
        
        mockURLSession.data = jsonData
        mockURLSession.response = HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        let result: User = try await apiClient.request(
            endpoint: "/test",
            method: .GET
        )
        
        // Then
        XCTAssertEqual(result.id, expectedUser.id)
        XCTAssertEqual(result.name, expectedUser.name)
        XCTAssertEqual(result.email, expectedUser.email)
    }
    
    func testRequest_UnauthorizedResponse_AttemptsTokenRefresh() async {
        // Given
        mockKeychainService.accessToken = "expired_token"
        mockKeychainService.refreshToken = "valid_refresh_token"
        
        mockURLSession.response = HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: 401,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        do {
            let _: User = try await apiClient.request(endpoint: "/test", method: .GET)
            XCTFail("Expected APIError.unauthorized")
        } catch APIError.unauthorized {
            // Then - Expected
            XCTAssertTrue(mockKeychainService.refreshTokenCalled)
        } catch {
            XCTFail("Unexpected error: \(error)")
        }
    }
    
    func testTryRefreshToken_ValidRefreshToken_ReturnsTrue() async {
        // Given
        mockKeychainService.refreshToken = "valid_refresh_token"
        let authResponse = AuthResponse(
            accessToken: "new_access_token",
            refreshToken: "new_refresh_token",
            user: User(id: "123", name: "Test", email: "test@example.com")
        )
        
        let jsonData = try! JSONEncoder().encode(authResponse)
        mockURLSession.data = jsonData
        mockURLSession.response = HTTPURLResponse(
            url: URL(string: "https://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        let result = await apiClient.tryRefreshToken()
        
        // Then
        XCTAssertTrue(result)
        XCTAssertEqual(mockKeychainService.savedAccessToken, "new_access_token")
        XCTAssertEqual(mockKeychainService.savedRefreshToken, "new_refresh_token")
    }
}

// Mock classes
class MockKeychainService: KeychainServiceProtocol {
    var accessToken: String?
    var refreshToken: String?
    var savedAccessToken: String?
    var savedRefreshToken: String?
    var refreshTokenCalled = false
    
    func getAccessToken() -> String? {
        return accessToken
    }
    
    func getRefreshToken() -> String? {
        return refreshToken
    }
    
    func saveTokens(accessToken: String, refreshToken: String) {
        savedAccessToken = accessToken
        savedRefreshToken = refreshToken
    }
    
    func clearTokens() {
        accessToken = nil
        refreshToken = nil
    }
}

class MockURLSession: URLSessionProtocol {
    var data: Data?
    var response: URLResponse?
    var error: Error?
    
    func data(for request: URLRequest) async throws -> (Data, URLResponse) {
        if let error = error {
            throw error
        }
        
        return (data ?? Data(), response ?? URLResponse())
    }
}
```

**Portfolio Service Tests:**
```swift
class PortfolioServiceTests: XCTestCase {
    
    var portfolioService: PortfolioService!
    var mockAPIClient: MockAPIClient!
    
    override func setUp() {
        super.setUp()
        mockAPIClient = MockAPIClient()
        portfolioService = PortfolioService()
        // Inject mock
    }
    
    func testFetchPortfolioSummary_Success_UpdatesPublishedProperty() async {
        // Given
        let expectedSummary = PortfolioSummary(
            totalValueTRY: 100000,
            todayChangePercent: 2.5,
            totalUnrealizedPLTRY: 5000,
            totalUnrealizedPLPercent: 5.26,
            status: "UP",
            estimatedProceedsTRY: 95000,
            costBasisTRY: 90000,
            unrealizedGainLossTRY: 5000,
            unrealizedGainLossPercent: 5.56,
            fxInfluenceTRY: 1000
        )
        
        mockAPIClient.portfolioSummaryResponse = expectedSummary
        
        // When
        await portfolioService.fetchPortfolioSummary()
        
        // Then
        XCTAssertEqual(portfolioService.portfolioSummary?.totalValueTRY, expectedSummary.totalValueTRY)
        XCTAssertEqual(portfolioService.portfolioSummary?.status, expectedSummary.status)
        XCTAssertFalse(portfolioService.isLoading)
        XCTAssertNil(portfolioService.errorMessage)
    }
    
    func testAddAcquisition_Success_AddsToAcquisitionsArray() async throws {
        // Given
        let acquisitionRequest = AcquisitionRequest(
            assetId: "asset123",
            quantity: 100,
            unitPrice: 50,
            currency: "USD",
            fee: nil,
            acquisitionDate: Date(),
            notes: nil,
            tags: nil
        )
        
        let expectedAcquisition = AcquisitionLot(
            id: "acq123",
            userId: "user123",
            assetId: "asset123",
            quantity: 100,
            unitPrice: 50,
            currency: "USD",
            fee: 0,
            acquisitionDate: Date(),
            notes: nil,
            tags: nil,
            createdAt: Date(),
            updatedAt: Date()
        )
        
        mockAPIClient.addAcquisitionResponse = expectedAcquisition
        
        // When
        let result = try await portfolioService.addAcquisition(acquisitionRequest)
        
        // Then
        XCTAssertEqual(result.id, expectedAcquisition.id)
        XCTAssertEqual(portfolioService.acquisitions.count, 1)
        XCTAssertEqual(portfolioService.acquisitions.first?.id, expectedAcquisition.id)
    }
}
```

### 3. UI Tests

**Files to create:**
- `ios-app/InvestmentTrackerUITests/AuthUITests.swift`

```swift
class AuthUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    func testLoginFlow_ValidCredentials_NavigatesToDashboard() {
        // Given
        let emailField = app.textFields["Email or Username"]
        let passwordField = app.secureTextFields["Password"]
        let loginButton = app.buttons["Sign In"]
        
        // When
        emailField.tap()
        emailField.typeText("test@example.com")
        
        passwordField.tap()
        passwordField.typeText("password123")
        
        loginButton.tap()
        
        // Then
        let dashboardTitle = app.navigationBars["Dashboard"]
        XCTAssertTrue(dashboardTitle.waitForExistence(timeout: 5))
    }
    
    func testSignUpFlow_ValidData_CreatesAccount() {
        // Given
        let signUpTab = app.buttons["Sign Up"]
        signUpTab.tap()
        
        let nameField = app.textFields["Full Name"]
        let emailField = app.textFields["Email"]
        let passwordField = app.secureTextFields["Password"]
        let confirmPasswordField = app.secureTextFields["Confirm Password"]
        let createAccountButton = app.buttons["Create Account"]
        
        // When
        nameField.tap()
        nameField.typeText("John Doe")
        
        emailField.tap()
        emailField.typeText("john@example.com")
        
        passwordField.tap()
        passwordField.typeText("password123")
        
        confirmPasswordField.tap()
        confirmPasswordField.typeText("password123")
        
        createAccountButton.tap()
        
        // Then
        let dashboardTitle = app.navigationBars["Dashboard"]
        XCTAssertTrue(dashboardTitle.waitForExistence(timeout: 5))
    }
    
    func testOAuthButtons_ArePresent_AndTappable() {
        // Given & When
        let googleButton = app.buttons["Continue with Google"]
        let appleButton = app.buttons["Continue with Apple"]
        
        // Then
        XCTAssertTrue(googleButton.exists)
        XCTAssertTrue(googleButton.isEnabled)
        XCTAssertTrue(appleButton.exists)
        XCTAssertTrue(appleButton.isEnabled)
    }
}
```

## 🤖 Android Testing Implementation

### 1. Test Dependencies

**Add to `android-app/app/build.gradle`:**
```kotlin
dependencies {
    // Unit Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.11.0'
    testImplementation 'org.mockito:mockito-inline:4.11.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:4.1.0'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'app.cash.turbine:turbine:1.0.0'
    
    // Instrumented Testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4:1.5.4'
    androidTestImplementation 'androidx.test:runner:1.5.2'
    androidTestImplementation 'androidx.test:rules:1.5.0'
    androidTestImplementation 'com.squareup.okhttp3:mockwebserver:4.12.0'
    
    debugImplementation 'androidx.compose.ui:ui-test-manifest:1.5.4'
}
```

### 2. Unit Tests

**Files to create:**
- `android-app/app/src/test/java/com/yuksel/investmenttracker/ui/auth/AuthViewModelTest.kt`

```kotlin
@ExperimentalCoroutinesApi
class AuthViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Mock
    private lateinit var apiService: ApiService
    
    @Mock
    private lateinit var tokenManager: TokenManager
    
    @Mock
    private lateinit var oauthManager: OAuthManager
    
    private lateinit var authViewModel: AuthViewModel
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        authViewModel = AuthViewModel(apiService, tokenManager, oauthManager)
    }
    
    @Test
    fun `login with valid credentials should update auth state`() = runTest {
        // Given
        val loginRequest = LoginRequest("test@example.com", "password123")
        val authResponse = AuthResponse(
            accessToken = "access_token",
            refreshToken = "refresh_token",
            user = User("1", "Test User", "test@example.com")
        )
        
        whenever(apiService.login(loginRequest)).thenReturn(authResponse)
        
        // When
        authViewModel.login(loginRequest)
        
        // Then
        authViewModel.authState.test {
            val state = awaitItem()
            assertEquals(true, state.isAuthenticated)
            assertEquals(authResponse.user, state.user)
            assertEquals(false, state.isLoading)
            assertNull(state.error)
        }
        
        verify(tokenManager).saveTokens(authResponse.accessToken, authResponse.refreshToken)
    }
    
    @Test
    fun `login with invalid credentials should set error state`() = runTest {
        // Given
        val loginRequest = LoginRequest("test@example.com", "wrong_password")
        val exception = RuntimeException("Invalid credentials")
        
        whenever(apiService.login(loginRequest)).thenThrow(exception)
        
        // When
        authViewModel.login(loginRequest)
        
        // Then
        authViewModel.authState.test {
            val state = awaitItem()
            assertEquals(false, state.isAuthenticated)
            assertEquals("Invalid credentials", state.error)
            assertEquals(false, state.isLoading)
        }
    }
    
    @Test
    fun `signInWithGoogle should handle success`() = runTest {
        // Given
        val authResponse = AuthResponse(
            accessToken = "google_access_token",
            refreshToken = "google_refresh_token",
            user = User("1", "Google User", "google@example.com")
        )
        
        whenever(oauthManager.signInWithGoogle(any())).thenReturn(authResponse)
        
        // When
        authViewModel.signInWithGoogle()
        
        // Then
        authViewModel.authState.test {
            val state = awaitItem()
            assertEquals(true, state.isAuthenticated)
            assertEquals(authResponse.user, state.user)
        }
        
        verify(tokenManager).saveTokens(authResponse.accessToken, authResponse.refreshToken)
    }
    
    @Test
    fun `forgotPassword should send reset email`() = runTest {
        // Given
        val email = "test@example.com"
        val successMessage = "Reset email sent"
        
        whenever(apiService.forgotPassword(email)).thenReturn(successMessage)
        
        // When
        authViewModel.forgotPassword(email)
        
        // Then
        authViewModel.authState.test {
            val state = awaitItem()
            assertEquals("Password reset instructions sent to your email", state.message)
            assertEquals(false, state.isLoading)
        }
    }
}
```

**Token Manager Tests:**
```kotlin
@ExperimentalCoroutinesApi
class TokenManagerTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var sharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var editor: SharedPreferences.Editor
    
    @Mock
    private lateinit var apiService: ApiService
    
    private lateinit var tokenManager: TokenManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)
        
        tokenManager = TokenManager(context, apiService)
        // Inject mocked SharedPreferences
    }
    
    @Test
    fun `saveTokens should store tokens in encrypted preferences`() = runTest {
        // Given
        val accessToken = "access_token"
        val refreshToken = "refresh_token"
        
        // When
        tokenManager.saveTokens(accessToken, refreshToken)
        
        // Then
        verify(editor).putString("access_token", accessToken)
        verify(editor).putString("refresh_token", refreshToken)
        verify(editor).apply()
    }
    
    @Test
    fun `refreshToken should return true on success`() = runTest {
        // Given
        val refreshToken = "valid_refresh_token"
        val authResponse = AuthResponse("new_access", "new_refresh", null)
        
        whenever(sharedPreferences.getString("refresh_token", null)).thenReturn(refreshToken)
        whenever(apiService.refreshToken(any())).thenReturn(authResponse)
        
        // When
        val result = tokenManager.refreshToken()
        
        // Then
        assertTrue(result)
        verify(editor).putString("access_token", "new_access")
        verify(editor).putString("refresh_token", "new_refresh")
    }
    
    @Test
    fun `refreshToken should return false on failure`() = runTest {
        // Given
        val refreshToken = "invalid_refresh_token"
        
        whenever(sharedPreferences.getString("refresh_token", null)).thenReturn(refreshToken)
        whenever(apiService.refreshToken(any())).thenThrow(RuntimeException("Invalid token"))
        
        // When
        val result = tokenManager.refreshToken()
        
        // Then
        assertFalse(result)
        verify(editor).remove("access_token")
        verify(editor).remove("refresh_token")
    }
}
```

### 3. UI Tests

**Files to create:**
- `android-app/app/src/androidTest/java/com/yuksel/investmenttracker/ui/AuthScreenTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setUp() {
        composeTestRule.setContent {
            InvestmentTrackerTheme {
                AuthScreen()
            }
        }
    }
    
    @Test
    fun loginForm_withValidInputs_enablesSignInButton() {
        // Given
        val emailField = composeTestRule.onNodeWithText("Email or Username")
        val passwordField = composeTestRule.onNodeWithText("Password")
        val signInButton = composeTestRule.onNodeWithText("Sign In")
        
        // When
        emailField.performTextInput("test@example.com")
        passwordField.performTextInput("password123")
        
        // Then
        signInButton.assertIsEnabled()
    }
    
    @Test
    fun loginForm_withEmptyInputs_disablesSignInButton() {
        // Given
        val signInButton = composeTestRule.onNodeWithText("Sign In")
        
        // Then
        signInButton.assertIsNotEnabled()
    }
    
    @Test
    fun switchToSignUp_showsSignUpForm() {
        // Given
        val signUpTab = composeTestRule.onNodeWithText("Sign Up")
        
        // When
        signUpTab.performClick()
        
        // Then
        composeTestRule.onNodeWithText("Full Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
    }
    
    @Test
    fun oauthButtons_areDisplayed_andClickable() {
        // Then
        composeTestRule.onNodeWithText("Continue with Google")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithText("Continue with Apple")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun forgotPasswordButton_isDisplayed_andClickable() {
        // Then
        composeTestRule.onNodeWithText("Forgot password?")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
```

**Navigation Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun navigationBetweenScreens_worksCorrectly() {
        composeTestRule.setContent {
            InvestmentTrackerTheme {
                InvestmentTrackerNavigation()
            }
        }
        
        // Test bottom navigation
        composeTestRule.onNodeWithText("Assets").performClick()
        composeTestRule.onNodeWithTag("AssetsScreen").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithTag("HistoryScreen").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithTag("SettingsScreen").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.onNodeWithTag("DashboardScreen").assertIsDisplayed()
    }
}
```

## 🚀 Test Automation and CI/CD

### 1. GitHub Actions Workflow

**Create `.github/workflows/test.yml`:**
```yaml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    
    services:
      mongodb:
        image: mongo:4.4.3
        ports:
          - 27017:27017
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Run backend tests
      run: |
        cd backend
        ./gradlew test
    
    - name: Run integration tests
      run: |
        cd backend
        ./gradlew integrationTest
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Backend Tests
        path: backend/build/test-results/test/*.xml
        reporter: java-junit
        
    - name: Upload coverage reports
      uses: codecov/codecov-action@v3
      with:
        file: backend/build/reports/jacoco/test/jacocoTestReport.xml

  ios-tests:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Select Xcode version
      run: sudo xcode-select -s /Applications/Xcode_15.0.app/Contents/Developer
    
    - name: Cache CocoaPods
      uses: actions/cache@v3
      with:
        path: ios-app/Pods
        key: ${{ runner.os }}-pods-${{ hashFiles('**/Podfile.lock') }}
        restore-keys: |
          ${{ runner.os }}-pods-
    
    - name: Install dependencies
      run: |
        cd ios-app
        pod install
    
    - name: Run iOS tests
      run: |
        cd ios-app
        xcodebuild test \
          -workspace InvestmentTracker.xcworkspace \
          -scheme InvestmentTracker \
          -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.0' \
          -resultBundlePath TestResults
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: failure()
      with:
        name: ios-test-results
        path: ios-app/TestResults.xcresult

  android-tests:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Run unit tests
      run: |
        cd android-app
        ./gradlew testDebugUnitTest
    
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 30
        target: google_apis
        arch: x86_64
        profile: Nexus 6
        script: |
          cd android-app
          ./gradlew connectedDebugAndroidTest
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: failure()
      with:
        name: android-test-results
        path: android-app/app/build/reports/androidTests/
```

## 📊 Coverage and Quality Gates

### 1. Backend Coverage Configuration

**Add to `backend/build.gradle`:**
```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.8"
}

test {
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                "**/config/**",
                "**/dto/**",
                "**/domain/entity/**",
                "**/*Application*"
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.8 // 80% coverage
            }
        }
    }
}
```

### 2. Quality Gates

**SonarQube configuration:**
```properties
# sonar-project.properties
sonar.projectKey=investment-tracker
sonar.organization=your-org
sonar.sources=backend/src/main,ios-app/InvestmentTracker,android-app/app/src/main
sonar.tests=backend/src/test,ios-app/InvestmentTrackerTests,android-app/app/src/test
sonar.java.coveragePlugin=jacoco
sonar.coverage.jacoco.xmlReportPaths=backend/build/reports/jacoco/test/jacocoTestReport.xml
sonar.exclusions=**/*Application*,**/dto/**,**/entity/**
```

## 🎯 Testing Success Criteria

After implementing the testing infrastructure:

### Backend Testing
- [ ] Unit test coverage > 80%
- [ ] Integration tests for all controllers
- [ ] Security tests for authentication
- [ ] Database tests with Testcontainers
- [ ] Mock external API dependencies

### iOS Testing  
- [ ] Unit tests for all services and view models
- [ ] UI tests for critical user flows
- [ ] OAuth integration tests
- [ ] Keychain service tests
- [ ] API client tests with mocked responses

### Android Testing
- [ ] Unit tests for ViewModels and repositories
- [ ] Compose UI tests for all screens
- [ ] Navigation tests
- [ ] OAuth integration tests
- [ ] EncryptedSharedPreferences tests

### CI/CD Pipeline
- [ ] Automated test execution on PR/push
- [ ] Test results reporting
- [ ] Coverage reporting
- [ ] Quality gates enforcement
- [ ] Failed test artifact collection

### Performance Testing
- [ ] API load testing
- [ ] Mobile app performance testing
- [ ] Memory leak detection
- [ ] Battery usage optimization verification