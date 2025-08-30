package com.yuksel.investmenttracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.yuksel.investmenttracker.data.model.analytics.AssetAllocationPoint
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AssetAllocationPieChart(
    data: List<AssetAllocationPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Asset Allocation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No allocation data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pie Chart
                    AndroidView(
                        factory = { context ->
                            PieChart(context).apply {
                                description.isEnabled = false
                                setUsePercentValues(true)
                                setEntryLabelColor(Color.Black.toArgb())
                                setEntryLabelTextSize(12f)
                                setDrawHoleEnabled(true)
                                setHoleColor(Color.Transparent.toArgb())
                                setHoleRadius(40f)
                                setTransparentCircleColor(Color.White.toArgb())
                                setTransparentCircleAlpha(110)
                                setTransparentCircleRadius(45f)
                                setDrawCenterText(true)
                                centerText = "Portfolio"
                                setCenterTextSize(16f)
                                setRotationAngle(0f)
                                isRotationEnabled = true
                                isHighlightPerTapEnabled = true
                                legend.isEnabled = false
                            }
                        },
                        modifier = Modifier
                            .size(150.dp)
                            .weight(1f),
                        update = { chart ->
                            val entries = data.map { allocation ->
                                PieEntry(
                                    allocation.percentage.toFloat(),
                                    allocation.assetName
                                )
                            }

                            val colors = data.map { allocation ->
                                getColorForAssetType(allocation.assetType).toArgb()
                            }

                            val dataSet = PieDataSet(entries, "").apply {
                                setColors(colors)
                                valueTextSize = 11f
                                valueTextColor = Color.White.toArgb()
                            }

                            chart.data = PieData(dataSet)
                            chart.invalidate()
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(data) { allocation ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .fillMaxWidth()
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(getColorForAssetType(allocation.assetType))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = allocation.assetName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${String.format("%.1f", allocation.percentage)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "₺${String.format("%.0f", allocation.value)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getColorForAssetType(type: String): Color {
    return when (type.uppercase()) {
        "EQUITY" -> Color(0xFF2196F3)
        "FX" -> Color(0xFF4CAF50)
        "PRECIOUS_METAL" -> Color(0xFFFF9800)
        "FUND" -> Color(0xFF9C27B0)
        else -> Color.Gray
    }
}