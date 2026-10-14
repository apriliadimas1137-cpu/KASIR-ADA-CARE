package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryItem
import com.example.data.model.StockLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import java.util.UUID

@Composable
fun InventoryScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val inventoryItems by viewModel.inventoryItems.collectAsState()
    val stockLogs by viewModel.stockLogs.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Stok Bahan, 1: Riwayat Pemakaian
    var selectedItemForRestock by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("inventory_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Warning Banner jika ada stok menipis
        if (lowStockItems.isNotEmpty()) {
            item {
                Surface(
                    color = WarningAmberSoft,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Peringatan: ${lowStockItems.size} Bahan Menipis!",
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Segera lakukan restock agar operasional cuci tidak terhambat.",
                                fontSize = 12.sp,
                                color = InkDark
                            )
                        }
                    }
                }
            }
        }

        // Header Title & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manajemen Stok Otomatis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stok berkurang otomatis saat order dibuat",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Bahan", fontSize = 12.sp)
                }
            }
        }

        // Sub Tabs
        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Daftar Bahan (${inventoryItems.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Riwayat Pemakaian & Log", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (activeTab == 0) {
            // Inventory List
            if (inventoryItems.isEmpty()) {
                item {
                    Text("Belum ada data bahan.", modifier = Modifier.padding(16.dp), color = InkMuted)
                }
            } else {
                items(inventoryItems, key = { it.id }) { item ->
                    InventoryCard(
                        item = item,
                        onRestockClick = { selectedItemForRestock = item },
                        onDeleteClick = { viewModel.deleteInventoryItem(item) }
                    )
                }
            }
        } else {
            // Stock Logs History (Menunjukkan pemotongan otomatis)
            if (stockLogs.isEmpty()) {
                item {
                    Text("Belum ada riwayat mutasi stok.", modifier = Modifier.padding(16.dp), color = InkMuted)
                }
            } else {
                items(stockLogs, key = { it.id }) { log ->
                    StockLogRow(log = log)
                }
            }
        }
    }

    // Modal Restock Dialog
    selectedItemForRestock?.let { target ->
        var restockAmountText by remember { mutableStateOf("10") }
        var costText by remember { mutableStateOf(target.costPerUnit.toString()) }

        AlertDialog(
            onDismissRequest = { selectedItemForRestock = null },
            title = { Text("Restock: ${target.name}") },
            text = {
                Column {
                    Text(
                        text = "Stok saat ini: ${target.stock} ${target.unit}",
                        fontSize = 13.sp,
                        color = InkMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = restockAmountText,
                        onValueChange = { restockAmountText = it },
                        label = { Text("Jumlah Tambahan (${target.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it.filter { c -> c.isDigit() } },
                        label = { Text("Biaya Modal Beli per Satuan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = restockAmountText.toDoubleOrNull() ?: 0.0
                        val cost = costText.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            viewModel.restock(target.id, amount, cost)
                            Toast.makeText(context, "Stok ${target.name} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            selectedItemForRestock = null
                        }
                    }
                ) {
                    Text("Simpan Restock")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForRestock = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Tambah Bahan Baru
    if (showAddDialog) {
        var nameText by remember { mutableStateOf("") }
        var stockText by remember { mutableStateOf("10") }
        var minStockText by remember { mutableStateOf("3") }
        var unitText by remember { mutableStateOf("botol") }
        var costText by remember { mutableStateOf("50000") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Bahan / Perlengkapan Baru") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Nama Bahan / Perlengkapan *") },
                        placeholder = { Text("Contoh: Semir Sepatu Hitam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitText,
                            onValueChange = { unitText = it },
                            label = { Text("Satuan") },
                            placeholder = { Text("pcs/ml/botol") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it },
                        label = { Text("Batas Peringatan Minimum") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameText.isNotBlank()) {
                            val newItem = InventoryItem(
                                id = "inv_" + UUID.randomUUID().toString().take(8),
                                name = nameText.trim(),
                                stock = stockText.toDoubleOrNull() ?: 0.0,
                                minStock = minStockText.toDoubleOrNull() ?: 2.0,
                                unit = unitText.trim(),
                                costPerUnit = costText.toLongOrNull() ?: 0L
                            )
                            viewModel.saveInventoryItem(newItem)
                            Toast.makeText(context, "Bahan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun InventoryCard(
    item: InventoryItem,
    onRestockClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLow = item.isLowStock

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Minimal stok: ${item.minStock} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }

                Surface(
                    color = if (isLow) WarningAmberSoft else SuccessGreenSoft,
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
                                .background(if (isLow) WarningAmber else SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLow) "Stok Menipis" else "Aman",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLow) WarningAmber else SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar stok
            val maxDisplay = (item.minStock * 3.0).coerceAtLeast(10.0)
            val progress = (item.stock / maxDisplay).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isLow) WarningAmber else BrandTeal,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format("%.1f", item.stock)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLow) WarningAmber else InkDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = item.unit, fontSize = 13.sp, color = InkMuted)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = InkMuted, modifier = Modifier.size(18.dp))
                    }
                    Button(
                        onClick = onRestockClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockLogRow(log: StockLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.inventoryName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = log.reason,
                    fontSize = 11.sp,
                    color = InkMuted
                )
                Text(
                    text = DateUtils.formatDateTime(log.timestamp),
                    fontSize = 10.sp,
                    color = InkMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val isNegative = log.changeAmount < 0
                Text(
                    text = "${if (isNegative) "" else "+"}${String.format("%.2f", log.changeAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isNegative) ErrorRed else SuccessGreen
                )
                Text(
                    text = "Sisa: ${String.format("%.1f", log.resultingStock)}",
                    fontSize = 10.sp,
                    color = InkMuted
                )
            }
        }
    }
}
