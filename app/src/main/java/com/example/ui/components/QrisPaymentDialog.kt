package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ShopSettings
import com.example.ui.theme.*
import com.example.util.DateUtils
import com.example.util.QrisHelper
import kotlinx.coroutines.delay

@Composable
fun QrisPaymentDialog(
    orderId: String,
    amount: Long,
    shopSettings: ShopSettings,
    onPaymentConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    var isSimulatingSuccess by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(15 * 60) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && !isSimulatingSuccess) {
            delay(1000L)
            secondsLeft--
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val qrBitmap = remember(orderId, amount) {
        val payload = QrisHelper.generateDynamicQrisPayload(
            merchantName = shopSettings.qrisMerchantName,
            nmid = shopSettings.qrisNmid,
            amount = amount,
            orderId = orderId
        )
        QrisHelper.generateQrBitmap(payload, 500)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("qris_payment_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                                .background(BrandTealSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "QRIS Dinamis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standar Pembayaran Nasional",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = InkMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Merchant & Amount
                Surface(
                    color = BrandTealContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shopSettings.qrisMerchantName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandTeal
                        )
                        Text(
                            text = "NMID: ${shopSettings.qrisNmid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = DateUtils.formatCurrency(amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = InkDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Image Frame
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, LineBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QRIS Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Countdown Timer & Supported Apps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Kadaluarsa dalam: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "BCA • Mandiri • BRI • BNI • GoPay • OVO • DANA • ShopeePay",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Simulasi & Konfirmasi Button
                Button(
                    onClick = {
                        isSimulatingSuccess = true
                        onPaymentConfirmed()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_qris_payment_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verifikasi Pembayaran Berhasil",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bayar Nanti / Saat Pengambilan", color = InkMuted)
                }
            }
        }
    }
}
