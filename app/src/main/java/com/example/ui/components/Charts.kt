package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.*

data class DailyStat(
    val dateLabel: String,
    val dayName: String,
    val revenue: Long,
    val pairs: Int
)

@Composable
fun RevenueBarChart(
    dailyStats: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    if (dailyStats.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada data transaksi.", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxRevenue = dailyStats.maxOfOrNull { it.revenue }?.coerceAtLeast(50000L) ?: 50000L

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grafik Omzet 7 Hari Terakhir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Maks: ${DateUtils.formatCurrency(maxRevenue)}",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 30.dp.toPx()
                val barCount = dailyStats.size
                val barSpacing = size.width / barCount
                val barWidth = barSpacing * 0.55f

                // Draw horizontal guide lines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = chartHeight * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color(0xFFE0E8E8),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Bars
                dailyStats.forEachIndexed { index, stat ->
                    val fraction = (stat.revenue.toFloat() / maxRevenue).coerceIn(0f, 1f)
                    val barHeight = chartHeight * fraction
                    val x = index * barSpacing + (barSpacing - barWidth) / 2f
                    val y = chartHeight - barHeight

                    val brush = Brush.verticalGradient(
                        colors = listOf(BrandTealLight, BrandTeal),
                        startY = y,
                        endY = chartHeight
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            // X-Axis Day Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(26.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dailyStats.forEach { stat ->
                    Text(
                        text = stat.dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun VolumeChart(
    dailyStats: List<DailyStat>,
    dailyCapacity: Int,
    modifier: Modifier = Modifier
) {
    val maxPairs = (dailyStats.maxOfOrNull { it.pairs } ?: 0).coerceAtLeast(dailyCapacity + 4)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Volume Pasang Sepatu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentOrange)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kapasitas Toko: $dailyCapacity/hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 26.dp.toPx()
                val count = dailyStats.size
                val spacing = size.width / count
                val barWidth = spacing * 0.45f

                // Draw Capacity Target line
                val capY = chartHeight * (1f - (dailyCapacity.toFloat() / maxPairs).coerceIn(0f, 1f))
                drawLine(
                    color = AccentOrange.copy(alpha = 0.6f),
                    start = Offset(0f, capY),
                    end = Offset(size.width, capY),
                    strokeWidth = 2.dp.toPx()
                )

                dailyStats.forEachIndexed { index, stat ->
                    val fraction = (stat.pairs.toFloat() / maxPairs).coerceIn(0f, 1f)
                    val barHeight = chartHeight * fraction
                    val x = index * spacing + (spacing - barWidth) / 2f
                    val y = chartHeight - barHeight

                    val color = if (stat.pairs > dailyCapacity) AccentOrange else SuccessGreen

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }

            // Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(24.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dailyStats.forEach { stat ->
                    Text(
                        text = "${stat.pairs} p",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodBreakdown(
    methodBreakdown: Map<String, Long>,
    totalRevenue: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Sebaran Metode Pembayaran",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (totalRevenue == 0L || methodBreakdown.isEmpty()) {
            Text("Belum ada data pembayaran.", color = InkMuted, style = MaterialTheme.typography.bodySmall)
            return
        }

        val colors = listOf(BrandTeal, AccentOrange, SuccessGreen, Color(0xFF7A5AF8), Color(0xFF0070BA))

        // Progress Stack Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            var colorIdx = 0
            methodBreakdown.forEach { (_, amount) ->
                val weight = (amount.toFloat() / totalRevenue).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(colors[colorIdx % colors.size])
                )
                colorIdx++
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend Items
        var colorIdx = 0
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            methodBreakdown.forEach { (method, amount) ->
                val percentage = if (totalRevenue > 0) (amount * 100 / totalRevenue) else 0
                val color = colors[colorIdx % colors.size]
                colorIdx++

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DateUtils.formatCurrency(amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($percentage%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }
                }
            }
        }
    }
}
