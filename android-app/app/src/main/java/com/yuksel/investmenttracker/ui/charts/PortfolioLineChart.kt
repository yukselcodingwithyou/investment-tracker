package com.yuksel.investmenttracker.ui.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.yuksel.investmenttracker.data.model.analytics.PortfolioHistoryPoint
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioLineChart(
    data: List<PortfolioHistoryPoint>,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("30D") }
    val periods = listOf("7D", "30D", "90D", "1Y", "ALL")

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio Value",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Period Selector
                SingleChoiceSegmentedButtonRow {
                    periods.forEachIndexed { index, period ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                            onClick = { selectedPeriod = period },
                            selected = selectedPeriod == period
                        ) {
                            Text(period)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No chart data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                            setPinchZoom(true)
                            setDrawGridBackground(false)
                            
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                valueFormatter = object : ValueFormatter() {
                                    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                                    override fun getFormattedValue(value: Float): String {
                                        return try {
                                            val date = Date(value.toLong())
                                            dateFormat.format(date)
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }
                                }
                            }
                            
                            axisLeft.apply {
                                setDrawGridLines(true)
                                granularity = 1f
                            }
                            
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    update = { chart ->
                        val entries = data.mapIndexed { _, point ->
                            Entry(
                                point.date.toEpochDay().toFloat(),
                                point.value.toFloat()
                            )
                        }

                        val dataSet = LineDataSet(entries, "Portfolio Value").apply {
                            color = Color.Blue.toArgb()
                            setCircleColor(Color.Blue.toArgb())
                            lineWidth = 2f
                            circleRadius = 3f
                            setDrawCircleHole(false)
                            valueTextSize = 9f
                            setDrawFilled(true)
                            fillColor = Color.Blue.toArgb()
                            fillAlpha = 50
                        }

                        chart.data = LineData(dataSet)
                        chart.invalidate()
                    }
                )
            }
        }
    }
}