package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Service
import com.example.data.model.ShopSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DateUtils
import java.util.UUID

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.shopSettings.collectAsState()
    val services by viewModel.services.collectAsState()

    var shopName by remember(settings) { mutableStateOf(settings.shopName) }
    var shopAddress by remember(settings) { mutableStateOf(settings.shopAddress) }
    var shopPhone by remember(settings) { mutableStateOf(settings.shopPhone) }
    var capacityText by remember(settings) { mutableStateOf(settings.dailyCapacity.toString()) }
    var overloadText by remember(settings) { mutableStateOf(settings.overloadDays.toString()) }
    var autoDelay by remember(settings) { mutableStateOf(settings.autoQueueDelay) }
    var qrisMerchant by remember(settings) { mutableStateOf(settings.qrisMerchantName) }
    var qrisNmid by remember(settings) { mutableStateOf(settings.qrisNmid) }
    var offDay by remember(settings) { mutableIntStateOf(settings.offDay) }

    var showAddServiceDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pengaturan Toko & Layanan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Section: Profil Toko
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Informasi Toko",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Nama Toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("Alamat Toko (Tampil di Nota)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { shopPhone = it },
                        label = { Text("No. WhatsApp Toko") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: Kapasitas & Perhitungan Estimasi
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kapasitas & Estimasi Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sistem secara otomatis menyesuaikan tanggal selesai berdasarkan antrian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it.filter { c -> c.isDigit() } },
                        label = { Text("Kapasitas Cuci per Hari (Pasang)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = overloadText,
                        onValueChange = { overloadText = it.filter { c -> c.isDigit() } },
                        label = { Text("Penyesuaian Overload Manual (+Hari)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Otomatis geser hari jika antrian melebihi kapasitas")
                        Switch(
                            checked = autoDelay,
                            onCheckedChange = { autoDelay = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hari Libur Selector
                    Text(text = "Hari Libur Toko:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val offDays = listOf(-1 to "Tidak Ada", 0 to "Minggu", 1 to "Senin", 5 to "Jumat")
                        offDays.forEach { (code, name) ->
                            FilterChip(
                                selected = offDay == code,
                                onClick = { offDay = code },
                                label = { Text(name, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Section: Konfigurasi Pembayaran QRIS
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Konfigurasi QRIS & Rekening",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = qrisMerchant,
                        onValueChange = { qrisMerchant = it },
                        label = { Text("Nama Merchant QRIS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = qrisNmid,
                        onValueChange = { qrisNmid = it },
                        label = { Text("NMID QRIS Toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Settings Button
        item {
            Button(
                onClick = {
                    val updated = settings.copy(
                        shopName = shopName.trim(),
                        shopAddress = shopAddress.trim(),
                        shopPhone = shopPhone.trim(),
                        dailyCapacity = capacityText.toIntOrNull() ?: 12,
                        overloadDays = overloadText.toIntOrNull() ?: 0,
                        autoQueueDelay = autoDelay,
                        offDay = offDay,
                        qrisMerchantName = qrisMerchant.trim(),
                        qrisNmid = qrisNmid.trim()
                    )
                    viewModel.saveShopSettings(updated)
                    Toast.makeText(context, "Pengaturan toko berhasil disimpan!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Perubahan Pengaturan", fontWeight = FontWeight.Bold)
            }
        }

        // Section: Manajemen Layanan
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Layanan Cuci (${services.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { showAddServiceDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 12.sp)
                }
            }
        }

        items(services, key = { it.id }) { svc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = svc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "${DateUtils.formatCurrency(svc.price)} • Pengerjaan ${svc.durationDays} hari",
                            fontSize = 12.sp,
                            color = InkMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = svc.isActive,
                            onCheckedChange = { checked ->
                                viewModel.saveService(svc.copy(isActive = checked))
                            }
                        )
                        IconButton(onClick = { viewModel.deleteService(svc) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = InkMuted)
                        }
                    }
                }
            }
        }
    }

    // Modal Tambah Layanan
    if (showAddServiceDialog) {
        var sName by remember { mutableStateOf("") }
        var sPrice by remember { mutableStateOf("45000") }
        var sDays by remember { mutableStateOf("2") }

        AlertDialog(
            onDismissRequest = { showAddServiceDialog = false },
            title = { Text("Tambah Layanan Baru") },
            text = {
                Column {
                    OutlinedTextField(
                        value = sName,
                        onValueChange = { sName = it },
                        label = { Text("Nama Layanan *") },
                        placeholder = { Text("Contoh: Leather Treatment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sPrice,
                        onValueChange = { sPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sDays,
                        onValueChange = { sDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Lama Pengerjaan (Hari)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sName.isNotBlank()) {
                            val newSvc = Service(
                                id = "L_" + UUID.randomUUID().toString().take(6),
                                name = sName.trim(),
                                price = sPrice.toLongOrNull() ?: 35000L,
                                durationDays = sDays.toIntOrNull() ?: 2,
                                isActive = true
                            )
                            viewModel.saveService(newSvc)
                            Toast.makeText(context, "Layanan baru ditambahkan!", Toast.LENGTH_SHORT).show()
                            showAddServiceDialog = false
                        }
                    }
                ) {
                    Text("Simpan Layanan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
