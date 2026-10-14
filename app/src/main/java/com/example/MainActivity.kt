package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.components.ReceiptModal
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

enum class AppNavTab(val route: String, val title: String) {
    POS("pos", "Kasir"),
    QUEUE("queue", "Antrian"),
    INVENTORY("inventory", "Stok"),
    REPORTS("reports", "Laporan"),
    SETTINGS("settings", "Setelan")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.util.NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(AppNavTab.POS) }

                val activeQueue by viewModel.activeQueue.collectAsState()
                val lowStockItems by viewModel.lowStockItems.collectAsState()
                val selectedOrder by viewModel.selectedOrder.collectAsState()
                val settings by viewModel.shopSettings.collectAsState()

                BackHandler(enabled = currentTab != AppNavTab.POS) {
                    currentTab = AppNavTab.POS
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(R.drawable.logo_ada_care),
                                        contentDescription = "Logo ADA Care Studio",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = settings.shopName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Shoes Care & Restoration",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = InkMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (activeQueue.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = BrandTealSoft,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandTeal)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${activeQueue.size} antrian",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandTealDark
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppNavTab.POS,
                                onClick = { currentTab = AppNavTab.POS },
                                icon = {
                                    Icon(
                                        if (currentTab == AppNavTab.POS) Icons.Default.PointOfSale else Icons.Outlined.PointOfSale,
                                        contentDescription = "Kasir"
                                    )
                                },
                                label = { Text("Kasir") },
                                modifier = Modifier.testTag("nav_item_pos")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppNavTab.QUEUE,
                                onClick = { currentTab = AppNavTab.QUEUE },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (activeQueue.isNotEmpty()) {
                                                Badge { Text("${activeQueue.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (currentTab == AppNavTab.QUEUE) Icons.Default.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                                            contentDescription = "Antrian"
                                        )
                                    }
                                },
                                label = { Text("Antrian") },
                                modifier = Modifier.testTag("nav_item_queue")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppNavTab.INVENTORY,
                                onClick = { currentTab = AppNavTab.INVENTORY },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (lowStockItems.isNotEmpty()) {
                                                Badge(containerColor = WarningAmber) {
                                                    Text("!")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (currentTab == AppNavTab.INVENTORY) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                                            contentDescription = "Stok"
                                        )
                                    }
                                },
                                label = { Text("Stok") },
                                modifier = Modifier.testTag("nav_item_inventory")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppNavTab.REPORTS,
                                onClick = { currentTab = AppNavTab.REPORTS },
                                icon = {
                                    Icon(
                                        if (currentTab == AppNavTab.REPORTS) Icons.Default.InsertChart else Icons.Outlined.InsertChart,
                                        contentDescription = "Laporan"
                                    )
                                },
                                label = { Text("Laporan") },
                                modifier = Modifier.testTag("nav_item_reports")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppNavTab.SETTINGS,
                                onClick = { currentTab = AppNavTab.SETTINGS },
                                icon = {
                                    Icon(
                                        if (currentTab == AppNavTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Setelan"
                                    )
                                },
                                label = { Text("Setelan") },
                                modifier = Modifier.testTag("nav_item_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                AppNavTab.POS -> PosScreen(
                                    viewModel = viewModel,
                                    onOrderCreated = { order ->
                                        viewModel.selectOrder(order)
                                    }
                                )
                                AppNavTab.QUEUE -> QueueScreen(
                                    viewModel = viewModel,
                                    onSelectOrder = { order ->
                                        viewModel.selectOrder(order)
                                    }
                                )
                                AppNavTab.INVENTORY -> InventoryScreen(
                                    viewModel = viewModel
                                )
                                AppNavTab.REPORTS -> ReportScreen(
                                    viewModel = viewModel
                                )
                                AppNavTab.SETTINGS -> SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // Modal Dialog Rincian Nota & Cetak / Share WhatsApp
                    selectedOrder?.let { order ->
                        ReceiptModal(
                            order = order,
                            shopSettings = settings,
                            onStatusAdvance = { nextStatus ->
                                viewModel.advanceOrderStatus(order.id, nextStatus)
                            },
                            onPaymentToggled = { isPaid ->
                                viewModel.updateOrderPayment(order.id, isPaid)
                            },
                            onDeleteOrder = {
                                viewModel.deleteOrder(order.id)
                            },
                            onDismiss = {
                                viewModel.selectOrder(null)
                            }
                        )
                    }
                }
            }
        }
    }
}
