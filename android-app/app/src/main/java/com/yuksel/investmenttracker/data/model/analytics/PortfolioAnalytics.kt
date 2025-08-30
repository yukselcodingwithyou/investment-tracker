package com.yuksel.investmenttracker.data.model.analytics

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class PortfolioHistoryPoint(
    @SerializedName("date") val date: LocalDate,
    @SerializedName("value") val value: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("changePercent") val changePercent: Double
)

data class AssetAllocationPoint(
    @SerializedName("assetType") val assetType: String,
    @SerializedName("assetName") val assetName: String,
    @SerializedName("value") val value: Double,
    @SerializedName("percentage") val percentage: Double,
    @SerializedName("color") val color: String
)

data class TopMover(
    @SerializedName("assetId") val assetId: String,
    @SerializedName("assetSymbol") val assetSymbol: String,
    @SerializedName("assetName") val assetName: String,
    @SerializedName("currentPrice") val currentPrice: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("changePercent") val changePercent: Double,
    @SerializedName("value") val value: Double,
    @SerializedName("direction") val direction: String
)

data class PortfolioAnalytics(
    @SerializedName("portfolioHistory") val portfolioHistory: List<PortfolioHistoryPoint>,
    @SerializedName("assetAllocation") val assetAllocation: List<AssetAllocationPoint>,
    @SerializedName("topMovers") val topMovers: List<TopMover>,
    @SerializedName("totalReturn") val totalReturn: Double?,
    @SerializedName("totalReturnPercent") val totalReturnPercent: Double?,
    @SerializedName("volatility") val volatility: Double?,
    @SerializedName("sharpeRatio") val sharpeRatio: Double?,
    @SerializedName("maxDrawdown") val maxDrawdown: Double?
)