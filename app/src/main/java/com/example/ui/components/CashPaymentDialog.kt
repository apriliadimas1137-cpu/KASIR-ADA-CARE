package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.DateUtils

@Composable
fun CashPaymentDialog(
    totalBill: Long,
    onPaymentConfirmed: (received: Long, change: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var receivedText by remember { mutableStateOf(totalBill.toString()) }
    val receivedAmount = receivedText.toLongOrNull() ?: 0L
    val change = receivedAmount - totalBill
    val isEnough = receivedAmount >= totalBill

    val quickDenominations = remember(totalBill) {
        val list = mutableListOf<Long>()
        list.add(totalBill) // Uang Pas
        val steps = listOf(20000L, 50000L, 100000L, 150000L, 200000L, 500000L)
        for (step in steps) {
            if (step > totalBill && !list.contains(step)) {
                list.add(step)
            }
        }
        list.take(4)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("cash_payment_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SuccessGreenSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pembayaran Tunai",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = InkMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tagihan
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Tagihan:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkMuted
                        )
                        Text(
                            text = DateUtils.formatCurrency(totalBill),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Uang Diterima
                OutlinedTextField(
                    value = receivedText,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        receivedText = digitsOnly
                    },
                    label = { Text("Uang Diterima dari Pelanggan") },
                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_received_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Denominasi Cepat
                Text(
                    text = "Pilihan Cepat:",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickDenominations.forEach { denom ->
                        val label = if (denom == totalBill) "Pas" else DateUtils.formatCurrency(denom).replace("Rp", "")
                        OutlinedButton(
                            onClick = { receivedText = denom.toString() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kembalian Box
                Surface(
                    color = if (isEnough) SuccessGreenSoft else ErrorRedSoft,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnough) "Uang Kembalian:" else "Uang Kurang:",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isEnough) SuccessGreen else ErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = DateUtils.formatCurrency(if (isEnough) change else (totalBill - receivedAmount)),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isEnough) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm Button
                Button(
                    onClick = {
                        if (isEnough) {
                            onPaymentConfirmed(receivedAmount, change)
                        }
                    },
                    enabled = isEnough,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_cash_payment_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Lunas & Simpan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
