package com.yuksel.investmenttracker.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.yuksel.investmenttracker.data.model.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "investment_tracker_tokens",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }
    
    // Flow to notify when authentication expires
    private val _authenticationExpired = MutableSharedFlow<Unit>()
    val authenticationExpired: SharedFlow<Unit> = _authenticationExpired.asSharedFlow()
    
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit()
            .putString(ACCESS_TOKEN_KEY, accessToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .apply()
    }
    
    suspend fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }
    
    suspend fun getRefreshToken(): String? {
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }
    
    suspend fun clearTokens() {
        sharedPreferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .apply()
    }
    
    suspend fun refreshToken(): Boolean {
        val refreshToken = getRefreshToken() ?: return false
        
        return try {
            // In a real implementation, this would make an HTTP call to refresh the token
            // We avoid circular dependencies by using a separate service or provider pattern
            // For now, we'll implement the structure without the API call
            
            // TODO: Implement actual refresh token API call using a provider pattern
            // This could be done through:
            // 1. A separate RefreshTokenProvider that doesn't depend on AuthInterceptor
            // 2. Using OkHttp directly without interceptors for the refresh call
            // 3. Using a different HTTP client instance specifically for token refresh
            
            // For now, we'll simulate a successful refresh for testing
            // In production, this should be replaced with actual API integration
            
            // Simulate network delay
            kotlinx.coroutines.delay(1000)
            
            // Mock successful response - in real implementation, parse actual API response
            val mockAccessToken = "new_access_token_${System.currentTimeMillis()}"
            val mockRefreshToken = "new_refresh_token_${System.currentTimeMillis()}"
            
            saveTokens(mockAccessToken, mockRefreshToken)
            
            true
            
        } catch (e: Exception) {
            // Refresh failed, clear tokens and notify authentication expired
            clearTokens()
            
            // Notify that authentication has expired (user needs to re-login)
            _authenticationExpired.emit(Unit)
            
            false
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
}