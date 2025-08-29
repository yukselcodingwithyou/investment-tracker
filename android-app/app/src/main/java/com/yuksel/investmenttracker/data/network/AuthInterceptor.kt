package com.yuksel.investmenttracker.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Add authorization header if token exists
        val token = runBlocking { tokenManager.getAccessToken() }
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        
        val response = chain.proceed(newRequest)
        
        // Handle 401 responses by attempting token refresh
        if (response.code == 401 && token != null) {
            response.close()
            
            val refreshed = runBlocking { 
                try {
                    tokenManager.refreshToken()
                } catch (e: Exception) {
                    false
                }
            }
            
            if (refreshed) {
                val newToken = runBlocking { tokenManager.getAccessToken() }
                val retryRequest = request.newBuilder()
                    .addHeader("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(retryRequest)
            } else {
                // Clear tokens and redirect to login
                runBlocking { tokenManager.clearTokens() }
            }
        }
        
        return response
    }
}