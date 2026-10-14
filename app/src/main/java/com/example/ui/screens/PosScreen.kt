package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.DiscountPromo
import com.example.data.model.Order
import com.example.data.model.PaymentMethod
import com.example.data.model.Service
import com.example.ui.components.CashPaymentDialog
import com.example.ui.components.QrisPaymentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    onOrderCreated: (Order) -> Unit
) {
    val context = LocalContext.current
    val draft by viewModel.draft.collectAsState()
    val services by viewModel.services.collectAsState()
    val activeServices = remember(services) { services.filter { it.isActive } }
    val settings by viewModel.shopSettings.collectAsState()
    val activeQueue by viewModel.activeQueue.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val registeredCustomers by viewModel.registeredCustomers.collectAsState()

    var showQrisDialog by remember { mutableStateOf(false) }
    var showCashDialog by remember { mutableStateOf(false) }
    var showCustomDiscountDialog by remember { mutableStateOf(false) }
    var showPromoBroadcastDialog by remember { mutableStateOf(false) }

    // Launcher izin notifikasi lokal (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Izin notifikasi lokal aktif!", Toast.LENGTH_SHORT).show()
        }
    }

    // Hitung estimasi real-time untuk draft
    val maxDuration = draft.selectedLines.maxOfOrNull { it.durationDays } ?: 1
    val activePairs = activeQueue.sumOf { it.pairsCount }
    val (autoEstTimestamp, estReason) = DateUtils.calculateEstimate(
        baseDurationDays = maxDuration,
        activeQueuePairs = activePairs,
        newOrderPairs = draft.totalPairs,
        dailyCapacity = settings.dailyCapacity,
        autoQueueDelay = settings.autoQueueDelay,
        overloadDays = settings.overloadDays,
        offDay = settings.offDay
    )
    val displayEstDate = draft.manualEstimatedDate ?: autoEstTimestamp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("pos_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Broadcast Promo & Notifikasi Banner
        item {
            Surface(
                color = BrandTealContainer,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandTeal.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandTeal,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengingat Promo Pelanggan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BrandTealDark
                            )
                            Text(
                                text = "${registeredCustomers.size} pelanggan terdaftar di database",
                                fontSize = 12.sp,
                                color = InkMuted
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showPromoBroadcastDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim Promo", fontSize = 12.sp)
                    }
                }
            }
        }

        // Low Stock Alert Banner (jika ada bahan menipis)
        if (lowStockItems.isNotEmpty()) {
            item {
                Surface(
                    color = WarningAmberSoft,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Perhatian Stok Bahan Menipis (${lowStockItems.size} item)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = WarningAmber
                            )
                            Text(
                                text = lowStockItems.take(2).joinToString(", ") { "${it.name} (${it.stock} ${it.unit})" } +
                                        if (lowStockItems.size > 2) " dan ${lowStockItems.size - 2} lainnya" else "",
                                fontSize = 11.sp,
                                color = InkDark
                            )
                        }
                    }
                }
            }
        }

        // Section: Data Pelanggan
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Pelanggan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = draft.customerName,
                        onValueChange = { viewModel.updateDraftCustomer(it, draft.customerPhone) },
                        label = { Text("Nama Pelanggan *") },
                        placeholder = { Text("Contoh: Dimas Bagus") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = InkMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = draft.customerPhone,
                        onValueChange = { viewModel.updateDraftCustomer(draft.customerName, it) },
                        label = { Text("Nomor WhatsApp / HP") },
                        placeholder = { Text("Contoh: 081234567890") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = InkMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_phone"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Section: Pilihan Layanan Cuci
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih Layanan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ketuk untuk menambah",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeServices.forEach { svc ->
                            FilterChip(
                                selected = draft.selectedLines.any { it.serviceId == svc.id },
                                onClick = { viewModel.addServiceToDraft(svc) },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(text = "+ ${svc.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            text = "${DateUtils.formatCurrency(svc.price)} • ${svc.durationDays} hr",
                                            fontSize = 11.sp,
                                            color = BrandTeal
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section: Daftar Item Cucian & Detail Sepatu
        if (draft.selectedLines.isNotEmpty()) {
            item {
                Text(
                    text = "Rincian Item (${draft.totalPairs} Pasang)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(draft.selectedLines) { index, line ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.serviceName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${DateUtils.formatCurrency(line.price)} / pasang • ${line.durationDays} hari",
                                    fontSize = 12.sp,
                                    color = InkMuted
                                )
                            }

                            // Stepper Qty
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateItemQty(index, -1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${line.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateItemQty(index, 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input detail sepatu (Merk, Warna, Kondisi)
                        OutlinedTextField(
                            value = line.shoeNotes,
                            onValueChange = { viewModel.updateItemNotes(index, it) },
                            label = { Text("Keterangan Sepatu (Merk / Warna / Kondisi)") },
                            placeholder = { Text("Contoh: Nike Dunk Low Panda, sol menguning, noda kopi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Section: Menu Pilihan Promo / Diskon Otomatis
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Discount, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Menu Promo & Diskon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (draft.discountAmount > 0) {
                            TextButton(
                                onClick = { viewModel.clearDiscount() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Hapus Diskon", color = ErrorRed, fontSize = 12.sp)
                            }
                        }
                    }

                    Text(
                        text = "Pilih promo siap pakai agar tidak perlu hitung manual",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Promo Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DiscountPromo.DEFAULT_PROMOS) { promo ->
                            val isSelected = draft.appliedPromo?.id == promo.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) viewModel.clearDiscount() else viewModel.applyPromo(promo)
                                },
                                label = {
                                    Text(
                                        text = promo.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }

                        item {
                            FilterChip(
                                selected = draft.customDiscountAmount > 0,
                                onClick = { showCustomDiscountDialog = true },
                                label = {
                                    Text(
                                        text = "+ Diskon Khusus",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }

                    // Tampilan status diskon yang sedang aktif
                    if (draft.discountAmount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = SuccessGreenSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Promo: ${draft.discountName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                                Text(
                                    text = "-${DateUtils.formatCurrency(draft.discountAmount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Catatan Tim & Estimasi Selesai
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Catatan & Estimasi Selesai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = { viewModel.updateDraftNotes(it) },
                        label = { Text("Catatan Khusus untuk Tim Kasir / Pengerjaan") },
                        placeholder = { Text("Contoh: Hati-hati bahan suede jangan kena sikat kasar") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = BrandTealContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Estimasi Selesai Otomatis:",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandTealDark
                            )
                            Text(
                                text = DateUtils.formatFullDate(displayEstDate),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandTealDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = estReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                    }
                }
            }
        }

        // Section: Pembayaran & Total Biaya
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Jika ada diskon, tampilkan Subtotal & Potongan
                    if (draft.discountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal Layanan", color = InkMuted, fontSize = 14.sp)
                            Text(text = DateUtils.formatCurrency(draft.subtotalAmount), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Diskon (${draft.discountName})", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "-${DateUtils.formatCurrency(draft.discountAmount)}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = LineBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Biaya",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateUtils.formatCurrency(draft.totalAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = LineBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pilihan Pembayaran Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !draft.isPaid,
                            onClick = { viewModel.updateDraftPayment(false, PaymentMethod.CASH) },
                            label = { Text("Bayar Nanti") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = draft.isPaid && draft.paymentMethod == PaymentMethod.CASH,
                            onClick = {
                                if (draft.totalAmount > 0) {
                                    showCashDialog = true
                                } else {
                                    viewModel.updateDraftPayment(true, PaymentMethod.CASH)
                                }
                            },
                            label = { Text("Tunai / Cash") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = draft.isPaid && draft.paymentMethod == PaymentMethod.QRIS,
                            onClick = {
                                if (draft.totalAmount > 0) {
                                    showQrisDialog = true
                                } else {
                                    viewModel.updateDraftPayment(true, PaymentMethod.QRIS)
                                }
                            },
                            label = { Text("QRIS") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (draft.isPaid) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = SuccessGreenSoft,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status: LUNAS (${draft.paymentMethod.displayName})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tombol Submit Buat Nota
                    Button(
                        onClick = {
                            viewModel.submitDraftOrder { createdOrder ->
                                onOrderCreated(createdOrder)
                                Toast.makeText(context, "Nota SB-${createdOrder.id} Berhasil Dibuat!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = draft.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_order_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buat Nota & Cetak",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!draft.isValid) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Isi nama pelanggan dan pilih minimal 1 layanan cuci sepatu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog Custom Diskon
    if (showCustomDiscountDialog) {
        var nameInput by remember { mutableStateOf("Diskon Khusus") }
        var amountInput by remember { mutableStateOf("10000") }

        AlertDialog(
            onDismissRequest = { showCustomDiscountDialog = false },
            title = { Text("Input Diskon Khusus") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama / Keterangan Diskon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Nominal Potongan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            viewModel.applyCustomDiscount(nameInput.trim(), amount)
                            showCustomDiscountDialog = false
                        }
                    }
                ) {
                    Text("Terapkan Diskon")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDiscountDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Dialog Broadcast Promo & Notifikasi Lokal
    if (showPromoBroadcastDialog) {
        var selectedPromoIndex by remember { mutableIntStateOf(0) }
        val promos = DiscountPromo.DEFAULT_PROMOS
        val curPromo = promos[selectedPromoIndex]

        Dialog(onDismissRequest = { showPromoBroadcastDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pengingat Promo Cuci Sepatu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kirim notifikasi & WhatsApp ke pelanggan",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                        IconButton(onClick = { showPromoBroadcastDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pilihan Promo
                    Text(text = "Pilih Promo yang Ingin Dikirim:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(promos) { idx, p ->
                            FilterChip(
                                selected = selectedPromoIndex == idx,
                                onClick = { selectedPromoIndex = idx },
                                label = { Text(p.title, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action 1: Trigger Notifikasi Lokal Perangkat
                    Button(
                        onClick = {
                            val notifTitle = "👟 Promo ADA Care Studio: ${curPromo.title}!"
                            val notifMsg = "${curPromo.description}. Gunakan kode ${curPromo.code} untuk cuci sepatumu!"
                            val sent = NotificationHelper.sendLocalPromoNotification(
                                context = context,
                                title = notifTitle,
                                message = notifMsg
                            )
                            if (sent) {
                                Toast.makeText(context, "Notifikasi lokal berhasil dikirim ke perangkat!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Notifikasi lokal dikirim (cek status bar perangkat)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bunyikan Notifikasi Lokal di HP", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = LineBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Action 2: Kirim ke Pelanggan Terdaftar via WhatsApp
                    Text(
                        text = "Pelanggan Terdaftar di Database (${registeredCustomers.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (registeredCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada pelanggan terdaftar di database.\nPelanggan otomatis tercatat saat nota dibuat.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = InkMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(registeredCustomers) { (custName, custPhone) ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = custName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = custPhone, fontSize = 11.sp, color = InkMuted)
                                        }

                                        Button(
                                            onClick = {
                                                val promoMsg = WhatsAppHelper.buildPromoBroadcastMessage(
                                                    customerName = custName,
                                                    promoTitle = curPromo.title,
                                                    promoCode = curPromo.code,
                                                    promoDesc = curPromo.description,
                                                    shopSettings = settings
                                                )
                                                WhatsAppHelper.shareToWhatsApp(
                                                    context = context,
                                                    phoneNumber = custPhone,
                                                    message = promoMsg,
                                                    imageUri = null
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WA", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog QRIS
    if (showQrisDialog) {
        QrisPaymentDialog(
            orderId = "NEW",
            amount = draft.totalAmount,
            shopSettings = settings,
            onPaymentConfirmed = {
                viewModel.updateDraftPayment(true, PaymentMethod.QRIS)
                showQrisDialog = false
                Toast.makeText(context, "QRIS Berhasil Diverifikasi Lunas!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showQrisDialog = false }
        )
    }

    // Modal Dialog Tunai
    if (showCashDialog) {
        CashPaymentDialog(
            totalBill = draft.totalAmount,
            onPaymentConfirmed = { received, change ->
                viewModel.updateDraftPayment(true, PaymentMethod.CASH)
                showCashDialog = false
                Toast.makeText(context, "Pembayaran Tunai Diterima. Kembalian: ${DateUtils.formatCurrency(change)}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCashDialog = false }
        )
    }
}
