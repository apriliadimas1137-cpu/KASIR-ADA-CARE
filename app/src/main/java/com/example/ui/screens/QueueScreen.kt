package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LaundryStatus
import com.example.data.model.Order
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils

@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    onSelectOrder: (Order) -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    var selectedFilter by remember { mutableStateOf("aktif") }
    var searchQuery by remember { mutableStateOf("") }
    var showShiftDialog by remember { mutableStateOf(false) }

    val todayKey = remember { DateUtils.todayKey() }

    // Metrics
    val activeQueueOrders = remember(orders) { orders.filter { it.currentStatus.isQueue } }
    val readyOrders = remember(orders) { orders.filter { it.currentStatus == LaundryStatus.SIAP } }
    val pickedUpOrders = remember(orders) { orders.filter { it.currentStatus == LaundryStatus.DIAMBIL } }
    val todayOrdersCount = remember(orders) { orders.count { it.dateKey == todayKey } }
    val activePairsCount = remember(activeQueueOrders) { activeQueueOrders.sumOf { it.pairsCount } }

    val filteredList = remember(orders, selectedFilter, searchQuery) {
        orders.filter { order ->
            val matchFilter = when (selectedFilter) {
                "aktif" -> order.currentStatus.isQueue
                "siap" -> order.currentStatus == LaundryStatus.SIAP
                "diambil" -> order.currentStatus == LaundryStatus.DIAMBIL
                else -> true
            }
            val matchSearch = if (searchQuery.isBlank()) true else {
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                        order.id.contains(searchQuery, ignoreCase = true) ||
                        order.customerPhone.contains(searchQuery)
            }
            matchFilter && matchSearch
        }.sortedWith(
            compareBy<Order> { if (selectedFilter == "aktif") it.estimatedDate else -it.createdAt }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("queue_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Header KPIs
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Antrian",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryMetricItem("${activeQueueOrders.size}", "Dikerjakan", BrandTeal)
                        SummaryMetricItem("$activePairsCount", "Pasang Antri", AccentOrange)
                        SummaryMetricItem("${readyOrders.size}", "Siap Diambil", SuccessGreen)
                        SummaryMetricItem("$todayOrdersCount", "Masuk Hari Ini", InkDark)
                    }
                }
            }
        }

        // Search Bar & Filter Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama pelanggan / kode nota SB-...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = InkMuted) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_order_input"),
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Filter Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    Triple("aktif", "Dikerjakan", activeQueueOrders.size),
                    Triple("siap", "Siap Diambil", readyOrders.size),
                    Triple("diambil", "Sudah Diambil", pickedUpOrders.size),
                    Triple("semua", "Semua Nota", orders.size)
                )
                items(filters) { (key, label, count) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = {
                            Text("$label ($count)", fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal)
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        // Overload Adjustment Banner
        if (activeQueueOrders.isNotEmpty()) {
            item {
                Surface(
                    color = BrandTealSoft.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cucian lagi penuh?", fontSize = 12.sp, color = InkDark, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = { showShiftDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Geser Estimasi Antrian", fontSize = 12.sp, color = BrandTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Empty State
        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = InkMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada nota yang cocok" else "Belum ada nota di kategori ini",
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                        Text(
                            text = "Nota baru dapat dibuat melalui menu Kasir.",
                            fontSize = 12.sp,
                            color = InkMuted
                        )
                    }
                }
            }
        } else {
            // Orders List
            items(filteredList, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    onCardClick = { onSelectOrder(order) },
                    onAdvanceStatus = { nextStatus ->
                        viewModel.advanceOrderStatus(order.id, nextStatus)
                    }
                )
            }
        }
    }

    // Dialog Geser Estimasi
    if (showShiftDialog) {
        var daysText by remember { mutableStateOf("1") }
        var noteText by remember { mutableStateOf("Antrian pengerjaan sedang padat") }

        AlertDialog(
            onDismissRequest = { showShiftDialog = false },
            title = { Text("Geser Estimasi Semua Nota") },
            text = {
                Column {
                    Text(
                        text = "Tambah estimasi selesai untuk ${activeQueueOrders.size} nota yang masih dikerjakan:",
                        fontSize = 13.sp,
                        color = InkMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it.filter { c -> c.isDigit() } },
                        label = { Text("Geser Berapa Hari") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Alasan / Catatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = daysText.toIntOrNull() ?: 1
                        viewModel.shiftQueueOrders(days, noteText)
                        showShiftDialog = false
                    }
                ) {
                    Text("Geser Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricItem(value: String, label: String, color: Color) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = InkMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onCardClick: () -> Unit,
    onAdvanceStatus: (LaundryStatus) -> Unit
) {
    val nextStatus = order.currentStatus.next()
    val isLate = order.currentStatus.isQueue && order.estimatedDate < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("order_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SB-${order.id} • Antrian #${order.queueNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }

                Surface(
                    color = order.currentStatus.color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(order.currentStatus.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.currentStatus.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = order.currentStatus.color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shoe items summary
            val itemNames = order.items.joinToString(", ") { "${it.serviceName} x${it.quantity}" }
            Text(
                text = itemNames,
                fontSize = 13.sp,
                color = InkDark,
                fontWeight = FontWeight.Medium
            )

            // First shoe note if any
            val firstNote = order.items.firstOrNull { it.shoeNotes.isNotBlank() }?.shoeNotes
            if (!firstNote.isNullOrBlank()) {
                Text(
                    text = "• $firstNote",
                    fontSize = 11.sp,
                    color = InkMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5-segment Progress Bar
            ProgressBarSteps(order.currentStatus.stepIndex)

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info & actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isLate) "Lewat Estimasi!" else "Est. Selesai:",
                        fontSize = 11.sp,
                        color = if (isLate) ErrorRed else InkMuted,
                        fontWeight = if (isLate) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = DateUtils.formatShortDate(order.estimatedDate),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLate) ErrorRed else InkDark
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCardClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Nota", fontSize = 12.sp)
                    }

                    if (nextStatus != null) {
                        Button(
                            onClick = { onAdvanceStatus(nextStatus) },
                            colors = ButtonDefaults.buttonColors(containerColor = nextStatus.color),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (nextStatus == LaundryStatus.DIAMBIL) "Sudah Diambil" else "Lanjut: ${nextStatus.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBarSteps(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0..4) {
            val isOn = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isOn) BrandTeal else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
