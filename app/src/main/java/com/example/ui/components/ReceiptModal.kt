package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LaundryStatus
import com.example.data.model.Order
import com.example.data.model.ShopSettings
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.ReceiptGenerator
import com.example.util.SavedReceipt
import com.example.util.WhatsAppHelper

@Composable
fun ReceiptModal(
    order: Order,
    shopSettings: ShopSettings,
    onStatusAdvance: (LaundryStatus) -> Unit,
    onPaymentToggled: (Boolean) -> Unit,
    onDeleteOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var lastSavedReceipt by remember { mutableStateOf<SavedReceipt?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val nextStatus = order.currentStatus.next()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("receipt_modal_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandTealSoft,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = BrandTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Nota SB-${order.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Status: ${order.currentStatus.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = order.currentStatus.color,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Thermal Receipt Layout
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LineBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PaperReceipt)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header Nota
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.logo_ada_care),
                                    contentDescription = "Logo ADA Care Studio",
                                    modifier = Modifier
                                        .height(70.dp)
                                        .wrapContentWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ADA CARE STUDIO",
                                    fontFamily = SpaceMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF124B36),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "SHOES CARE & RESTORATION",
                                    fontFamily = SpaceMono,
                                    fontSize = 10.sp,
                                    color = InkMuted,
                                    textAlign = TextAlign.Center
                                )
                                if (shopSettings.shopAddress.isNotBlank()) {
                                    Text(
                                        text = shopSettings.shopAddress,
                                        fontFamily = SpaceMono,
                                        fontSize = 11.sp,
                                        color = InkMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (shopSettings.shopPhone.isNotBlank()) {
                                    Text(
                                        text = "WA: ${shopSettings.shopPhone}",
                                        fontFamily = SpaceMono,
                                        fontSize = 11.sp,
                                        color = InkMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = LineBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Metadata
                            ReceiptRow("No. Nota", "SB-${order.id}", isBold = true)
                            ReceiptRow("Tanggal", DateUtils.formatDateTime(order.createdAt))
                            ReceiptRow("Antrian", "#${order.queueNumber} (${DateUtils.formatShortDate(order.createdAt)})")
                            ReceiptRow("Pelanggan", order.customerName, isBold = true)
                            if (order.customerPhone.isNotBlank()) {
                                ReceiptRow("No. HP", order.customerPhone)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = LineBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Items
                            Text(
                                text = "LAYANAN & SEPATU",
                                fontFamily = SpaceMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = InkMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            order.items.forEach { item ->
                                ReceiptRow(
                                    "${item.serviceName} x${item.quantity}",
                                    DateUtils.formatCurrency(item.price * item.quantity),
                                    isBold = true
                                )
                                if (item.shoeNotes.isNotBlank()) {
                                    Text(
                                        text = "• ${item.shoeNotes}",
                                        fontFamily = SpaceMono,
                                        fontSize = 11.sp,
                                        color = InkMuted,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            if (order.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catatan: ${order.notes}",
                                    fontFamily = SpaceMono,
                                    fontSize = 11.sp,
                                    color = InkMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = LineBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Total & Status
                            val rawSubtotal = if (order.subtotalAmount > 0L) order.subtotalAmount else (order.totalAmount + order.discountAmount)
                            if (order.discountAmount > 0) {
                                ReceiptRow("Subtotal", DateUtils.formatCurrency(rawSubtotal))
                                ReceiptRow(
                                    label = "Diskon (${order.discountName.ifBlank { "Promo" }})",
                                    value = "-${DateUtils.formatCurrency(order.discountAmount)}",
                                    textColor = SuccessGreen,
                                    isBold = true
                                )
                            }
                            ReceiptRow(
                                "TOTAL",
                                DateUtils.formatCurrency(order.totalAmount),
                                isBold = true,
                                fontSize = 16.sp
                            )
                            ReceiptRow(
                                "Status Bayar",
                                if (order.isPaid) "LUNAS (${order.paymentMethod})" else "BELUM LUNAS",
                                isBold = true,
                                textColor = if (order.isPaid) SuccessGreen else ErrorRed
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = LineBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Estimasi Selesai: ${DateUtils.formatFullDate(order.estimatedDate)}",
                                fontFamily = SpaceMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PaperInk
                            )
                            if (order.estimatedNote.isNotBlank()) {
                                Text(
                                    text = order.estimatedNote,
                                    fontFamily = SpaceMono,
                                    fontSize = 11.sp,
                                    color = InkMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Saved Notification Banner
                    if (lastSavedReceipt != null) {
                        Surface(
                            color = SuccessGreenSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Nota Gambar Berhasil Disimpan!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Lokasi: ${lastSavedReceipt?.absolutePath}",
                                        fontSize = 11.sp,
                                        color = InkDark
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Action Buttons: WhatsApp & Simpan Nota
                    Button(
                        onClick = {
                            val bitmap = ReceiptGenerator.createReceiptBitmap(context, order, shopSettings)
                            val saved = ReceiptGenerator.saveReceiptToDevice(context, bitmap, order.id, isJpeg = false)
                            lastSavedReceipt = saved

                            val msg = WhatsAppHelper.buildOrderMessage(order, shopSettings)
                            WhatsAppHelper.shareToWhatsApp(
                                context = context,
                                phoneNumber = order.customerPhone,
                                message = msg,
                                imageUri = saved.fileUri
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim Nota Gambar via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button Generate & Simpan PNG
                        OutlinedButton(
                            onClick = {
                                isSaving = true
                                val bitmap = ReceiptGenerator.createReceiptBitmap(context, order, shopSettings)
                                val saved = ReceiptGenerator.saveReceiptToDevice(context, bitmap, order.id, isJpeg = false)
                                lastSavedReceipt = saved
                                isSaving = false
                                Toast.makeText(context, "Nota PNG tersimpan di Galeri / Perangkat!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_receipt_png_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan PNG", fontSize = 12.sp)
                        }

                        // Button Generate & Simpan JPEG
                        OutlinedButton(
                            onClick = {
                                isSaving = true
                                val bitmap = ReceiptGenerator.createReceiptBitmap(context, order, shopSettings)
                                val saved = ReceiptGenerator.saveReceiptToDevice(context, bitmap, order.id, isJpeg = true)
                                lastSavedReceipt = saved
                                isSaving = false
                                Toast.makeText(context, "Nota JPEG tersimpan di Galeri / Perangkat!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("save_receipt_jpeg_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan JPEG", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kabari Sepatu Siap (jika status Siap Diambil)
                    if (order.currentStatus == LaundryStatus.SIAP) {
                        Button(
                            onClick = {
                                val msg = WhatsAppHelper.buildReadyMessage(order, shopSettings)
                                WhatsAppHelper.shareToWhatsApp(
                                    context = context,
                                    phoneNumber = order.customerPhone,
                                    message = msg,
                                    imageUri = lastSavedReceipt?.fileUri
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kabari Pelanggan: Sepatu Siap Diambil", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Copy Text Button
                    OutlinedButton(
                        onClick = {
                            val msg = WhatsAppHelper.buildOrderMessage(order, shopSettings)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Nota SB-${order.id}", msg))
                            Toast.makeText(context, "Teks nota berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Teks Nota", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Workflow & Management Controls
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Kelola Status & Pembayaran",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (nextStatus != null) {
                                Button(
                                    onClick = { onStatusAdvance(nextStatus) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = nextStatus.color),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Lanjut ke: ${nextStatus.title}", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = order.isPaid,
                                    onClick = { onPaymentToggled(true) },
                                    label = { Text("Lunas") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !order.isPaid,
                                    onClick = { onPaymentToggled(false) },
                                    label = { Text("Belum Lunas") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete Order
                    TextButton(
                        onClick = onDeleteOrder,
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Nota Ini", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    textColor: Color = PaperInk
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = SpaceMono,
            fontSize = fontSize,
            color = InkMuted
        )
        Text(
            text = value,
            fontFamily = SpaceMono,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
