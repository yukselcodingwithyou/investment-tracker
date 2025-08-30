# Android App Completion Prompt

## 🎯 Objective
Complete all missing Android Jetpack Compose implementations for the Investment Tracker application to achieve production readiness.

## 🤖 Required Implementations

### 1. Refresh Token Implementation

**Task:** Implement automatic token refresh functionality

**File to modify:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/data/network/TokenManager.kt`

**Implementation:**
```kotlin
suspend fun refreshToken(): Boolean {
    val refreshToken = getRefreshToken() ?: return false
    
    return try {
        val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
        
        saveTokens(response.accessToken, response.refreshToken)
        true
        
    } catch (e: Exception) {
        // Refresh failed, clear tokens
        clearTokens()
        
        // Notify app to redirect to login
        _authenticationExpired.emit(Unit)
        
        false
    }
}

// Add authentication expired flow
private val _authenticationExpired = MutableSharedFlow<Unit>()
val authenticationExpired: SharedFlow<Unit> = _authenticationExpired.asSharedFlow()
```

**New models to create:**
```kotlin
// data/model/auth/RefreshTokenRequest.kt
data class RefreshTokenRequest(
    val refreshToken: String
)
```

### 2. OAuth Implementation (Google & Apple Sign-In)

**Files to create:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/data/oauth/OAuthManager.kt`
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/auth/OAuthViewModel.kt`

**Dependencies to add to `build.gradle`:**
```kotlin
dependencies {
    // Google Sign-In
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    
    // Apple Sign In (third-party library)
    implementation 'com.github.willowtreeapps:sign-in-with-apple:0.3'
}
```

**OAuthManager implementation:**
```kotlin
package com.yuksel.investmenttracker.data.oauth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleCallback
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleConfiguration
import com.willowtreeapps.signinwithapplebutton.SignInWithAppleService
import com.yuksel.investmenttracker.R
import com.yuksel.investmenttracker.data.network.ApiService
import com.yuksel.investmenttracker.data.model.auth.AuthResponse
import com.yuksel.investmenttracker.data.model.auth.OAuthLoginRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    private var googleSignInClient: GoogleSignInClient? = null
    private var appleSignInService: SignInWithAppleService? = null
    
    fun initializeGoogleSignIn(activity: AppCompatActivity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
            
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        return googleSignInClient!!
    }
    
    fun initializeAppleSignIn(): SignInWithAppleService {
        val configuration = SignInWithAppleConfiguration.Builder()
            .clientId(context.getString(R.string.apple_client_id))
            .redirectUri(context.getString(R.string.apple_redirect_uri))
            .scope("email name")
            .build()
            
        appleSignInService = SignInWithAppleService(configuration)
        return appleSignInService!!
    }
    
    suspend fun signInWithGoogle(activity: AppCompatActivity): AuthResponse {
        val client = googleSignInClient ?: initializeGoogleSignIn(activity)
        
        return suspendCancellableCoroutine { continuation ->
            val signInIntent = client.signInIntent
            val launcher = activity.activityResultRegistry.register(
                "google_sign_in",
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task, continuation)
            }
            
            launcher.launch(signInIntent)
        }
    }
    
    private fun handleGoogleSignInResult(
        completedTask: Task<GoogleSignInAccount>,
        continuation: kotlinx.coroutines.CancellableContinuation<AuthResponse>
    ) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            
            if (idToken != null) {
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val oauthRequest = OAuthLoginRequest(idToken, "GOOGLE")
                        val response = apiService.googleOAuth(oauthRequest)
                        continuation.resume(response)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            } else {
                continuation.resumeWithException(Exception("No ID token received"))
            }
        } catch (e: ApiException) {
            continuation.resumeWithException(e)
        }
    }
    
    suspend fun signInWithApple(): AuthResponse {
        val service = appleSignInService ?: initializeAppleSignIn()
        
        return suspendCancellableCoroutine { continuation ->
            service.show(object : SignInWithAppleCallback {
                override fun onSignInWithAppleSuccess(authorizationCode: String) {
                    kotlinx.coroutines.GlobalScope.launch {
                        try {
                            val oauthRequest = OAuthLoginRequest(authorizationCode, "APPLE")
                            val response = apiService.appleOAuth(oauthRequest)
                            continuation.resume(response)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
                
                override fun onSignInWithAppleFailure(error: Throwable) {
                    continuation.resumeWithException(error)
                }
                
                override fun onSignInWithAppleCancel() {
                    continuation.resumeWithException(Exception("Apple Sign In cancelled"))
                }
            })
        }
    }
}
```

**Update AuthScreen.kt to implement OAuth:**
```kotlin
// Replace TODO comments in AuthScreen.kt with:

// In LoginForm:
OutlinedButton(
    onClick = { 
        authViewModel.signInWithGoogle()
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text("Continue with Google")
    }
}

Button(
    onClick = { 
        authViewModel.signInWithApple()
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.onSurface
    )
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_apple),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(20.dp)
        )
        Text(
            "Continue with Apple",
            color = MaterialTheme.colorScheme.surface
        )
    }
}

// For forgot password:
TextButton(onClick = { 
    authViewModel.forgotPassword(emailOrUsername)
}) {
    Text("Forgot password?")
}
```

### 3. AuthViewModel OAuth Implementation

**File to modify:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/auth/AuthViewModel.kt`

**Add OAuth methods:**
```kotlin
fun signInWithGoogle() {
    viewModelScope.launch {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        try {
            val activity = getCurrentActivity() // You'll need to implement this
            val authResponse = oauthManager.signInWithGoogle(activity)
            
            tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
            
            _authState.value = _authState.value.copy(
                isLoading = false,
                isAuthenticated = true,
                user = authResponse.user
            )
            
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = e.message ?: "Google Sign In failed"
            )
        }
    }
}

fun signInWithApple() {
    viewModelScope.launch {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        try {
            val authResponse = oauthManager.signInWithApple()
            
            tokenManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)
            
            _authState.value = _authState.value.copy(
                isLoading = false,
                isAuthenticated = true,
                user = authResponse.user
            )
            
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = e.message ?: "Apple Sign In failed"
            )
        }
    }
}

fun forgotPassword(email: String) {
    viewModelScope.launch {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        try {
            apiService.forgotPassword(email)
            
            _authState.value = _authState.value.copy(
                isLoading = false,
                message = "Password reset instructions sent to your email"
            )
            
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to send reset email"
            )
        }
    }
}
```

### 4. Settings Screen Implementation

**File to modify:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/settings/SettingsScreen.kt`

**Create SettingsViewModel:**
```kotlin
// ui/settings/SettingsViewModel.kt
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authViewModel: AuthViewModel,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    fun changePassword() {
        // Implement password change
    }
    
    fun enableTwoFactor() {
        // Implement 2FA setup
    }
    
    fun changeCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateBaseCurrency(currency)
        }
    }
    
    fun changeTimezone(timezone: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateTimezone(timezone)
        }
    }
    
    fun enableNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateNotificationsEnabled(enabled)
        }
    }
    
    fun exportData() {
        // Implement data export
    }
    
    fun deleteAccount() {
        // Implement account deletion
    }
    
    fun logout() {
        authViewModel.logout()
    }
}
```

**Update SettingsScreen with implementations:**
```kotlin
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Security Section
        item {
            SettingsSection(title = "Security") {
                SettingsItem(
                    title = "Change Password",
                    icon = Icons.Default.Lock,
                    onClick = { showPasswordChangeDialog = true }
                )
                
                SettingsItem(
                    title = "Two-Factor Authentication",
                    icon = Icons.Default.Security,
                    onClick = { settingsViewModel.enableTwoFactor() }
                )
            }
        }
        
        // Preferences Section
        item {
            SettingsSection(title = "Preferences") {
                SettingsItem(
                    title = "Base Currency",
                    icon = Icons.Default.AttachMoney,
                    onClick = { showCurrencyPicker = true }
                )
                
                SettingsItem(
                    title = "Timezone",
                    icon = Icons.Default.Schedule,
                    onClick = { showTimezonePicker = true }
                )
                
                SettingsItem(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    onClick = { settingsViewModel.enableNotifications(true) }
                )
            }
        }
        
        // Data Section
        item {
            SettingsSection(title = "Data") {
                SettingsItem(
                    title = "Export Data",
                    icon = Icons.Default.Download,
                    onClick = { settingsViewModel.exportData() }
                )
                
                SettingsItem(
                    title = "Delete Account",
                    icon = Icons.Default.Delete,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteConfirmation = true }
                )
            }
        }
        
        // Logout
        item {
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { settingsViewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sign Out")
            }
        }
    }
    
    // Dialogs
    if (showPasswordChangeDialog) {
        PasswordChangeDialog(
            onDismiss = { showPasswordChangeDialog = false },
            onConfirm = { oldPassword, newPassword ->
                settingsViewModel.changePassword()
                showPasswordChangeDialog = false
            }
        )
    }
    
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            onDismiss = { showCurrencyPicker = false },
            onCurrencySelected = { currency ->
                settingsViewModel.changeCurrency(currency)
                showCurrencyPicker = false
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### 5. Portfolio Service Implementation

**Files to create:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/data/repository/PortfolioRepository.kt`
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/portfolio/PortfolioViewModel.kt`

**PortfolioRepository implementation:**
```kotlin
@Singleton
class PortfolioRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getPortfolioSummary(): PortfolioSummary {
        return apiService.getPortfolioSummary()
    }
    
    suspend fun addAcquisition(request: AcquisitionRequest): AcquisitionLot {
        return apiService.addAcquisition(request)
    }
    
    suspend fun getAcquisitions(): List<AcquisitionLot> {
        return apiService.getAcquisitions()
    }
    
    suspend fun getPortfolioHistory(period: String): List<PortfolioHistoryPoint> {
        return apiService.getPortfolioHistory(period)
    }
    
    suspend fun getAssetAllocation(): List<AssetAllocationPoint> {
        return apiService.getAssetAllocation()
    }
}

// Data models
data class PortfolioSummary(
    val totalValueTRY: BigDecimal,
    val todayChangePercent: BigDecimal?,
    val totalUnrealizedPLTRY: BigDecimal,
    val totalUnrealizedPLPercent: BigDecimal,
    val status: String,
    val estimatedProceedsTRY: BigDecimal,
    val costBasisTRY: BigDecimal,
    val unrealizedGainLossTRY: BigDecimal,
    val unrealizedGainLossPercent: BigDecimal,
    val fxInfluenceTRY: BigDecimal?
)

data class AcquisitionRequest(
    val assetId: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val currency: String? = null,
    val fee: BigDecimal? = null,
    val acquisitionDate: LocalDate,
    val notes: String? = null,
    val tags: List<String>? = null
)

data class AcquisitionLot(
    val id: String,
    val userId: String,
    val assetId: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val currency: String,
    val fee: BigDecimal,
    val acquisitionDate: LocalDate,
    val notes: String?,
    val tags: List<String>?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### 6. Add Acquisition Screen

**Files to create:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/portfolio/AddAcquisitionScreen.kt`
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/portfolio/AddAcquisitionViewModel.kt`

**AddAcquisitionScreen implementation:**
```kotlin
@Composable
fun AddAcquisitionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddAcquisitionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAssetPicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Add Acquisition") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = { viewModel.saveAcquisition() },
                    enabled = uiState.canSave && !uiState.isLoading
                ) {
                    Text("Save")
                }
            }
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Asset Selection
            item {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAssetPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.selectedAsset != null) 
                                    uiState.selectedAsset!!.symbol 
                                else "Select Asset",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (uiState.selectedAsset != null) {
                                Text(
                                    text = uiState.selectedAsset!!.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                }
            }
            
            // Transaction Details
            item {
                OutlinedTextField(
                    value = uiState.quantity,
                    onValueChange = viewModel::updateQuantity,
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                OutlinedTextField(
                    value = uiState.unitPrice,
                    onValueChange = viewModel::updateUnitPrice,
                    label = { Text("Unit Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = uiState.currency,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("USD", "EUR", "TRY", "GBP", "JPY").forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    viewModel.updateCurrency(currency)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                OutlinedTextField(
                    value = uiState.fee,
                    onValueChange = viewModel::updateFee,
                    label = { Text("Fee (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                DatePickerField(
                    value = uiState.acquisitionDate,
                    onValueChange = viewModel::updateAcquisitionDate,
                    label = "Acquisition Date"
                )
            }
            
            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notes (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Asset Picker Sheet
    if (showAssetPicker) {
        AssetPickerBottomSheet(
            onAssetSelected = { asset ->
                viewModel.selectAsset(asset)
                showAssetPicker = false
            },
            onDismiss = { showAssetPicker = false }
        )
    }
    
    // Handle navigation after successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }
}

@Composable
fun DatePickerField(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    label: String
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    
    OutlinedTextField(
        value = value.format(dateFormatter),
        onValueChange = { },
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                onValueChange(selectedDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
```

### 7. Asset Management Implementation

**Files to create:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/data/repository/AssetRepository.kt`
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/assets/AssetViewModel.kt`
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/assets/AssetPickerBottomSheet.kt`

**AssetRepository implementation:**
```kotlin
@Singleton
class AssetRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun searchAssets(
        query: String = "",
        type: AssetType? = null,
        page: Int = 0,
        size: Int = 20
    ): PagedResponse<Asset> {
        return apiService.searchAssets(query, type?.name, page, size)
    }
    
    suspend fun getAssetDetails(id: String): AssetDetail {
        return apiService.getAssetDetails(id)
    }
}

// Data models
data class Asset(
    val id: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    val currency: String
)

data class AssetDetail(
    val id: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    val currency: String,
    val currentPrice: BigDecimal?,
    val priceHistory: List<PricePoint>?,
    val description: String?
)

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Int,
    val totalPages: Int,
    val size: Int,
    val number: Int
)

enum class AssetType {
    PRECIOUS_METAL,
    FX,
    EQUITY,
    FUND;
    
    val displayName: String
        get() = when (this) {
            PRECIOUS_METAL -> "Precious Metal"
            FX -> "Foreign Exchange"
            EQUITY -> "Equity"
            FUND -> "Fund"
        }
}
```

### 8. Charts Implementation

**Files to create:**
- `android-app/app/src/main/java/com/yuksel/investmenttracker/ui/charts/PortfolioChartScreen.kt`

**Dependencies to add:**
```kotlin
dependencies {
    // Charts library
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    // Or alternative: implementation 'co.yml:ycharts:2.1.0'
}
```

**Chart implementation:**
```kotlin
@Composable
fun PortfolioChartScreen(
    viewModel: ChartViewModel = hiltViewModel()
) {
    val chartData by viewModel.chartData.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Period Selector
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            TimePeriod.values().forEachIndexed { index, period ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TimePeriod.values().size),
                    onClick = { viewModel.selectPeriod(period) },
                    selected = period == selectedPeriod
                ) {
                    Text(period.displayName)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Portfolio Value Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            if (chartData.portfolioHistory.isNotEmpty()) {
                LineChart(
                    data = chartData.portfolioHistory,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Asset Allocation Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            if (chartData.assetAllocation.isNotEmpty()) {
                PieChart(
                    data = chartData.assetAllocation,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: List<PortfolioHistoryPoint>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                axisRight.isEnabled = false
            }
        },
        modifier = modifier,
        update = { chart ->
            val entries = data.mapIndexed { index, point ->
                Entry(index.toFloat(), point.value.toFloat())
            }
            
            val dataSet = LineDataSet(entries, "Portfolio Value").apply {
                color = Color.BLUE
                setCircleColor(Color.BLUE)
                lineWidth = 2f
                circleRadius = 3f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawFilled(true)
                fillColor = Color.BLUE
                fillAlpha = 50
            }
            
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
```

## 📦 Dependencies to Add

**Add to `android-app/app/build.gradle`:**
```kotlin
dependencies {
    // OAuth
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    implementation 'com.github.willowtreeapps:sign-in-with-apple:0.3'
    
    // Charts
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // Date/Time
    implementation 'org.threeten:threetenbp:1.6.8'
    
    // Additional UI components
    implementation 'androidx.compose.material3:material3-window-size-class:1.1.2'
}
```

## ⚙️ Configuration

**Add to `res/values/strings.xml`:**
```xml
<resources>
    <string name="google_web_client_id">YOUR_GOOGLE_CLIENT_ID</string>
    <string name="apple_client_id">YOUR_APPLE_CLIENT_ID</string>
    <string name="apple_redirect_uri">YOUR_APPLE_REDIRECT_URI</string>
</resources>
```

**Add icons to `res/drawable/`:**
- `ic_google.xml`
- `ic_apple.xml`

## 🧪 Testing Requirements

**Create test files:**
- `OAuthManagerTest.kt`
- `AuthViewModelTest.kt`
- `PortfolioRepositoryTest.kt`
- `AssetRepositoryTest.kt`
- `SettingsViewModelTest.kt`

## 🎯 Success Criteria

After implementing these changes:
- [ ] All TODO comments resolved
- [ ] OAuth login functional for Google and Apple
- [ ] Token refresh working automatically
- [ ] Forgot password flow complete
- [ ] Settings screen fully functional
- [ ] Add acquisition form working
- [ ] Asset search and management working
- [ ] Portfolio charts displaying data
- [ ] Comprehensive test coverage
- [ ] Smooth navigation and UX
- [ ] Error handling and loading states implemented