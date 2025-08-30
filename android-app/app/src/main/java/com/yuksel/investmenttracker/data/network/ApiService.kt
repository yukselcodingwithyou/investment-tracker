package com.yuksel.investmenttracker.data.network

import com.yuksel.investmenttracker.data.model.AcquisitionRequest
import com.yuksel.investmenttracker.data.model.AuthResponse
import com.yuksel.investmenttracker.data.model.LoginRequest
import com.yuksel.investmenttracker.data.model.PortfolioSummary
import com.yuksel.investmenttracker.data.model.RefreshTokenRequest
import com.yuksel.investmenttracker.data.model.SignUpRequest
import com.yuksel.investmenttracker.data.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    
    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): AuthResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): AuthResponse
    
    @GET("auth/me")
    suspend fun getCurrentUser(): User
    
    @GET("portfolio/summary")
    suspend fun getPortfolioSummary(): PortfolioSummary
    
    @POST("portfolio/acquisitions")
    suspend fun addAcquisition(@Body request: AcquisitionRequest): AcquisitionRequest
}