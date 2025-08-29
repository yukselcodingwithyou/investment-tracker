package com.yuksel.investmenttracker.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val providers: List<OAuthProvider>,
    val baseCurrency: String,
    val timezone: String,
    val createdAt: String // Will be parsed to LocalDateTime
)

@Serializable
enum class OAuthProvider {
    GOOGLE, APPLE
}

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: User
)

@Serializable
data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)

@Serializable
data class SignUpRequest(
    val name: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

@Serializable
data class PortfolioSummary(
    val totalValueTRY: Double,
    val todayChangePercent: Double,
    val totalUnrealizedPLTRY: Double,
    val totalUnrealizedPLPercent: Double,
    val status: String,
    val estimatedProceedsTRY: Double,
    val costBasisTRY: Double,
    val unrealizedGainLossTRY: Double,
    val unrealizedGainLossPercent: Double,
    val fxInfluenceTRY: Double
)

@Serializable
data class Asset(
    val id: String,
    val symbol: String,
    val name: String,
    val type: AssetType,
    val currency: String,
    val quantity: Double? = null,
    val quantityUnit: String? = null,
    val currentPrice: Double? = null,
    val dayChangePercent: Double? = null,
    val averageAcquisitionPrice: Double? = null,
    val unrealizedPLTRY: Double? = null,
    val unrealizedPLPercent: Double? = null,
    val marketValueTRY: Double? = null
)

@Serializable
enum class AssetType {
    PRECIOUS_METAL, FX, EQUITY, FUND;
    
    val displayName: String
        get() = when (this) {
            PRECIOUS_METAL -> "Precious Metal"
            FX -> "FX"
            EQUITY -> "Equity"
            FUND -> "Fund"
        }
}

@Serializable
data class AcquisitionRequest(
    val assetType: AssetType,
    val assetSymbol: String,
    val assetName: String? = null,
    val quantity: Double,
    val unitPrice: Double,
    val currency: String? = null,
    val fee: Double? = null,
    val acquisitionDate: String, // ISO date
    val notes: String? = null,
    val tags: List<String>? = null
)