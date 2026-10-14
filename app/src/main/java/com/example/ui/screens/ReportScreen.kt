package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LaundryStatus
import com.example.ui.components.PaymentMethodBreakdown
import com.example.ui.components.RevenueBarChart
import com.example.ui.components.VolumeChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.WhatsAppHelper

@Composable
fun ReportScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsState()
    val settings by viewModel.shopSettings.collectAsState()

    val kpi = remember(orders) { viewModel.calculateKpi(orders) }
    val dailyStats = remember(orders) { viewModel.calculate7DaysStats(orders) }
    val paymentBreakdown = remember(orders) { viewModel.calculatePaymentBreakdown(orders) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("report_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Laporan & Grafik Real-Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Statistik omzet, volume, & metode pembayaran",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }

                // Share Laporan ke WhatsApp Pemilik
                IconButton(
                    onClick = {
                        val reportText = """
                            📊 *LAPORAN KASIR CUCI SEPATU*
                            Toko: *${settings.shopName}*
                            Tanggal: ${DateUtils.formatFullDate(System.currentTimeMillis())}
                            --------------------------------
                            💰 Omzet Hari Ini: ${DateUtils.formatCurrency(kpi.todayRevenue)}
                            📈 Total Omzet: ${DateUtils.formatCurrency(kpi.totalRevenue)}
                            ⏳ Belum Lunas (Piutang): ${DateUtils.formatCurrency(kpi.unpaidAmount)} (${kpi.unpaidOrdersCount} nota)
                            👟 Antrian Dikerjakan: ${kpi.activeQueueCount} nota (${kpi.activePairsCount} pasang)
                            ✅ Siap Diambil: ${kpi.readyOrdersCount} nota
                            --------------------------------
                            Laporan otomatis digenerate dari POS Kasir Cuci Sepatu.
                        """.trimIndent()

                        WhatsAppHelper.shareToWhatsApp(
                            context = context,
                            phoneNumber = settings.shopPhone,
                            message = reportText,
                            imageUri = null
                        )
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Kirim Laporan", tint = BrandTeal)
                }
            }
        }

        // KPI Summary Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Omzet Hari Ini",
                        value = DateUtils.formatCurrency(kpi.todayRevenue),
                        subtext = "Transaksi lunas hari ini",
                        containerColor = BrandTealContainer,
                        valueColor = BrandTealDark,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total Omzet",
                        value = DateUtils.formatCurrency(kpi.totalRevenue),
                        subtext = "${orders.count { it.isPaid }} nota lunas",
                        containerColor = MaterialTheme.colorScheme.surface,
                        valueColor = InkDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Belum Lunas",
                        value = DateUtils.formatCurrency(kpi.unpaidAmount),
                        subtext = "${kpi.unpaidOrdersCount} nota belum bayar",
                        containerColor = if (kpi.unpaidOrdersCount > 0) ErrorRedSoft else MaterialTheme.colorScheme.surface,
                        valueColor = if (kpi.unpaidOrdersCount > 0) ErrorRed else InkDark,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Antrian Aktif",
                        value = "${kpi.activePairsCount} Pasang",
                        subtext = "${kpi.activeQueueCount} nota sedang dicuci",
                        containerColor = MaterialTheme.colorScheme.surface,
                        valueColor = AccentOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Chart 1: Revenue Bar Chart (7 Hari)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                RevenueBarChart(
                    dailyStats = dailyStats,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Chart 2: Pairs Volume Chart vs Daily Capacity
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                VolumeChart(
                    dailyStats = dailyStats,
                    dailyCapacity = settings.dailyCapacity,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Chart 3: Sebaran Metode Pembayaran
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethodBreakdown(
                    methodBreakdown = paymentBreakdown,
                    totalRevenue = kpi.totalRevenue,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Status Funnel Distribution
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Distribusi Status Cucian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LaundryStatus.entries.forEach { st ->
                        val count = orders.count { it.status == st.key }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(st.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = st.title, fontSize = 13.sp, color = InkDark)
                            }
                            Text(
                                text = "$count nota",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    containerColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = InkMuted
            )
        }
    }
}
