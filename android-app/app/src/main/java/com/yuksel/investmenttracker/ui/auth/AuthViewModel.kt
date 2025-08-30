package com.yuksel.investmenttracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuksel.investmenttracker.data.model.AuthResponse
import com.yuksel.investmenttracker.data.model.LoginRequest
import com.yuksel.investmenttracker.data.model.SignUpRequest
import com.yuksel.investmenttracker.data.model.User
import com.yuksel.investmenttracker.data.network.ApiService
import com.yuksel.investmenttracker.data.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthenticationStatus()
    }
    
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            val isLoggedIn = tokenManager.isLoggedIn()
            if (isLoggedIn) {
                try {
                    val user = apiService.getCurrentUser()
                    _authState.value = _authState.value.copy(
                        isAuthenticated = true,
                        user = user
                    )
                } catch (e: Exception) {
                    tokenManager.clearTokens()
                    _authState.value = _authState.value.copy(
                        isAuthenticated = false,
                        errorMessage = "Session expired"
                    )
                }
            }
        }
    }
    
    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val request = SignUpRequest(name, email, password, confirmPassword)
                val response = apiService.signUp(request)
                handleAuthResponse(response)
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Sign up failed"
                )
            }
        }
    }
    
    fun login(emailOrUsername: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val request = LoginRequest(emailOrUsername, password)
                val response = apiService.login(request)
                handleAuthResponse(response)
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            _authState.value = AuthState()
        }
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
    
    fun handleOAuthError(errorMessage: String) {
        _authState.value = _authState.value.copy(errorMessage = errorMessage)
    }
    
    private suspend fun handleAuthResponse(response: AuthResponse) {
        tokenManager.saveTokens(response.accessToken, response.refreshToken)
        _authState.value = _authState.value.copy(
            isAuthenticated = true,
            isLoading = false,
            user = response.user,
            errorMessage = null
        )
    }
}