package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "shop_settings")

class LaundryRepository(
    private val database: AppDatabase,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val orderDao = database.orderDao()
    private val serviceDao = database.serviceDao()
    private val inventoryDao = database.inventoryDao()
    private val stockLogDao = database.stockLogDao()

    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val activeQueueOrders: Flow<List<Order>> = orderDao.getActiveQueueOrders()
    val allServices: Flow<List<Service>> = serviceDao.getAllServices()
    val activeServices: Flow<List<Service>> = serviceDao.getActiveServices()
    val allInventory: Flow<List<InventoryItem>> = inventoryDao.getAllInventory()
    val lowStockItems: Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()
    val recentStockLogs: Flow<List<StockLog>> = stockLogDao.getRecentLogs()

    // Preferences keys
    private object PreferencesKeys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val DAILY_CAPACITY = intPreferencesKey("daily_capacity")
        val OVERLOAD_DAYS = intPreferencesKey("overload_days")
        val AUTO_QUEUE_DELAY = booleanPreferencesKey("auto_queue_delay")
        val OFF_DAY = intPreferencesKey("off_day")
        val QRIS_MERCHANT = stringPreferencesKey("qris_merchant")
        val QRIS_NMID = stringPreferencesKey("qris_nmid")
        val BANK_ACCOUNT = stringPreferencesKey("bank_account")
    }

    val shopSettings: StateFlow<ShopSettings> = context.dataStore.data
        .map { prefs ->
            ShopSettings(
                shopName = prefs[PreferencesKeys.SHOP_NAME] ?: "ADA Care Studio",
                shopAddress = prefs[PreferencesKeys.SHOP_ADDRESS] ?: "Jl. Veteran No. 18, Jakarta",
                shopPhone = prefs[PreferencesKeys.SHOP_PHONE] ?: "081234567890",
                dailyCapacity = prefs[PreferencesKeys.DAILY_CAPACITY] ?: 12,
                overloadDays = prefs[PreferencesKeys.OVERLOAD_DAYS] ?: 0,
                autoQueueDelay = prefs[PreferencesKeys.AUTO_QUEUE_DELAY] ?: true,
                offDay = prefs[PreferencesKeys.OFF_DAY] ?: -1,
                qrisMerchantName = prefs[PreferencesKeys.QRIS_MERCHANT] ?: "ADA CARE STUDIO",
                qrisNmid = prefs[PreferencesKeys.QRIS_NMID] ?: "ID1020030040050",
                bankAccount = prefs[PreferencesKeys.BANK_ACCOUNT] ?: "BCA 8820192831 a/n ADA Care Studio"
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, ShopSettings(shopName = "ADA Care Studio", qrisMerchantName = "ADA CARE STUDIO"))

    init {
        scope.launch {
            seedDefaultDataIfEmpty()
        }
    }

    suspend fun saveShopSettings(settings: ShopSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOP_NAME] = settings.shopName
            prefs[PreferencesKeys.SHOP_ADDRESS] = settings.shopAddress
            prefs[PreferencesKeys.SHOP_PHONE] = settings.shopPhone
            prefs[PreferencesKeys.DAILY_CAPACITY] = settings.dailyCapacity
            prefs[PreferencesKeys.OVERLOAD_DAYS] = settings.overloadDays
            prefs[PreferencesKeys.AUTO_QUEUE_DELAY] = settings.autoQueueDelay
            prefs[PreferencesKeys.OFF_DAY] = settings.offDay
            prefs[PreferencesKeys.QRIS_MERCHANT] = settings.qrisMerchantName
            prefs[PreferencesKeys.QRIS_NMID] = settings.qrisNmid
            prefs[PreferencesKeys.BANK_ACCOUNT] = settings.bankAccount
        }
    }

    private suspend fun seedDefaultDataIfEmpty() {
        if (serviceDao.getCount() == 0) {
            val defaultServices = listOf(
                Service(
                    id = "L1",
                    name = "Fast Clean",
                    price = 35000L,
                    durationDays = 1,
                    category = "CLEANING",
                    materialUsageJson = "[{\"inventoryId\":\"inv_foam\",\"amountPerPair\":0.04},{\"inventoryId\":\"inv_parfum\",\"amountPerPair\":0.03},{\"inventoryId\":\"inv_bag\",\"amountPerPair\":1.0},{\"inventoryId\":\"inv_silica\",\"amountPerPair\":1.0}]",
                    sortOrder = 1
                ),
                Service(
                    id = "L2",
                    name = "Deep Clean & Treatment",
                    price = 55000L,
                    durationDays = 3,
                    category = "CLEANING",
                    materialUsageJson = "[{\"inventoryId\":\"inv_foam\",\"amountPerPair\":0.08},{\"inventoryId\":\"inv_parfum\",\"amountPerPair\":0.05},{\"inventoryId\":\"inv_bag\",\"amountPerPair\":1.0},{\"inventoryId\":\"inv_silica\",\"amountPerPair\":1.0}]",
                    sortOrder = 2
                ),
                Service(
                    id = "L3",
                    name = "Unyellowing Midsole",
                    price = 75000L,
                    durationDays = 4,
                    category = "RESTORATION",
                    materialUsageJson = "[{\"inventoryId\":\"inv_unyellow\",\"amountPerPair\":0.1},{\"inventoryId\":\"inv_foam\",\"amountPerPair\":0.04},{\"inventoryId\":\"inv_bag\",\"amountPerPair\":1.0}]",
                    sortOrder = 3
                ),
                Service(
                    id = "L4",
                    name = "Leather Care & Polish",
                    price = 65000L,
                    durationDays = 2,
                    category = "TREATMENT",
                    materialUsageJson = "[{\"inventoryId\":\"inv_leather\",\"amountPerPair\":0.08},{\"inventoryId\":\"inv_parfum\",\"amountPerPair\":0.04},{\"inventoryId\":\"inv_bag\",\"amountPerPair\":1.0}]",
                    sortOrder = 4
                ),
                Service(
                    id = "L5",
                    name = "Repaint Sneaker Full",
                    price = 120000L,
                    durationDays = 5,
                    category = "RESTORATION",
                    materialUsageJson = "[{\"inventoryId\":\"inv_paint\",\"amountPerPair\":0.15},{\"inventoryId\":\"inv_bag\",\"amountPerPair\":1.0}]",
                    sortOrder = 5
                ),
                Service(
                    id = "L6",
                    name = "Reglue Sol Sepatu",
                    price = 50000L,
                    durationDays = 3,
                    category = "REPAIR",
                    materialUsageJson = "[{\"inventoryId\":\"inv_glue\",\"amountPerPair\":0.2},{\"inventoryId\":\"inv_bag\",\"amountPerPair\":1.0}]",
                    sortOrder = 6
                )
            )
            serviceDao.insertServices(defaultServices)
        }

        if (inventoryDao.getCount() == 0) {
            val defaultInventory = listOf(
                InventoryItem("inv_foam", "Cleaner Sneaker Foaming 500ml", 12.0, 3.0, "botol", 65000L),
                InventoryItem("inv_parfum", "Parfum Sepatu Anti-Bakteri", 8.0, 2.0, "botol", 45000L),
                InventoryItem("inv_unyellow", "Krim Oxidant Unyellowing 250ml", 6.0, 2.0, "botol", 55000L),
                InventoryItem("inv_leather", "Leather Balm Conditioner", 5.0, 2.0, "pot", 70000L),
                InventoryItem("inv_paint", "Cat Akrilik Leather Putih/Hitam", 4.0, 1.0, "botol", 40000L),
                InventoryItem("inv_glue", "Lem Sepatu Super Bond", 6.0, 2.0, "tube", 35000L),
                InventoryItem("inv_bag", "Dust Bag / Tas Spunbond Sepatu", 50.0, 15.0, "pcs", 2500L),
                InventoryItem("inv_silica", "Silica Gel Anti-Lembap", 100.0, 25.0, "sachet", 500L)
            )
            inventoryDao.insertInventories(defaultInventory)
        }
    }

    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        items: List<OrderItem>,
        isPaid: Boolean,
        paymentMethod: String,
        manualEstimatedDate: Long? = null,
        notes: String = "",
        discountAmount: Long = 0L,
        discountName: String = ""
    ): Order {
        val settings = shopSettings.value
        val todayKey = DateUtils.todayKey()
        val todayOrders = orderDao.getOrdersByDateKey(todayKey)
        val queueNumber = todayOrders.size + 1

        val subtotalAmount = items.sumOf { it.price * it.quantity }
        val finalTotalAmount = maxOf(0L, subtotalAmount - discountAmount)
        val pairsCount = items.sumOf { it.quantity }
        val maxDuration = items.maxOfOrNull { it.durationDays } ?: 1

        val activeOrders = orderDao.getOrdersByDateKey(todayKey)
        val activePairs = activeOrders.filter { LaundryStatus.fromKey(it.status).isQueue }.sumOf { it.pairsCount }

        val (autoEstDate, estReason) = DateUtils.calculateEstimate(
            baseDurationDays = maxDuration,
            activeQueuePairs = activePairs,
            newOrderPairs = pairsCount,
            dailyCapacity = settings.dailyCapacity,
            autoQueueDelay = settings.autoQueueDelay,
            overloadDays = settings.overloadDays,
            offDay = settings.offDay
        )

        val finalEstDate = manualEstimatedDate ?: autoEstDate

        // Generate short random code: e.g. 7K2M9P
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val codeBuilder = StringBuilder()
        val random = UUID.randomUUID().toString().replace("-", "")
        for (i in 0 until 6) {
            val idx = (random[i].code + i * 3) % chars.length
            codeBuilder.append(chars[idx])
        }
        val orderId = codeBuilder.toString()

        val logObj = JSONObject().apply {
            put(LaundryStatus.DITERIMA.key, System.currentTimeMillis())
        }

        val order = Order(
            id = orderId,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            status = LaundryStatus.DITERIMA.key,
            createdAt = System.currentTimeMillis(),
            dateKey = todayKey,
            queueNumber = queueNumber,
            estimatedDate = finalEstDate,
            estimatedDateString = DateUtils.formatFullDate(finalEstDate),
            estimatedNote = estReason,
            subtotalAmount = subtotalAmount,
            discountAmount = discountAmount,
            discountName = discountName,
            totalAmount = finalTotalAmount,
            pairsCount = pairsCount,
            isPaid = isPaid,
            paymentMethod = paymentMethod,
            paidAt = if (isPaid) System.currentTimeMillis() else null,
            notes = notes.trim(),
            itemsJson = OrderItem.listToJson(items),
            statusLogJson = logObj.toString()
        )

        orderDao.insertOrder(order)

        // OTOMATISASI MANAJEMEN STOK: Potong stok bahan secara otomatis berdasarkan pesanan
        deductStockForOrder(order, items)

        return order
    }

    private suspend fun deductStockForOrder(order: Order, items: List<OrderItem>) {
        for (item in items) {
            val service = serviceDao.getServiceById(item.serviceId)
            val deductions = service?.materialDeductions ?: emptyList()

            // Jika service memiliki deductions konfigurasi
            if (deductions.isNotEmpty()) {
                for (d in deductions) {
                    val totalDeduct = d.amountPerPair * item.quantity
                    inventoryDao.deductStock(d.inventoryId, totalDeduct)
                    val updated = inventoryDao.getInventoryById(d.inventoryId)
                    stockLogDao.insertLog(
                        StockLog(
                            inventoryId = d.inventoryId,
                            inventoryName = updated?.name ?: d.inventoryId,
                            changeAmount = -totalDeduct,
                            resultingStock = updated?.stock ?: 0.0,
                            reason = "Pemakaian Order SB-${order.id} (${item.serviceName})",
                            orderId = order.id
                        )
                    )
                }
            } else {
                // Fallback default deductions: Tas spunbond + Silica pack per pasang sepatu
                val bags = item.quantity.toDouble()
                inventoryDao.deductStock("inv_bag", bags)
                val invBag = inventoryDao.getInventoryById("inv_bag")
                stockLogDao.insertLog(
                    StockLog(
                        inventoryId = "inv_bag",
                        inventoryName = invBag?.name ?: "Dust Bag",
                        changeAmount = -bags,
                        resultingStock = invBag?.stock ?: 0.0,
                        reason = "Packaging Order SB-${order.id}",
                        orderId = order.id
                    )
                )

                inventoryDao.deductStock("inv_silica", bags)
                val invSilica = inventoryDao.getInventoryById("inv_silica")
                stockLogDao.insertLog(
                    StockLog(
                        inventoryId = "inv_silica",
                        inventoryName = invSilica?.name ?: "Silica Gel",
                        changeAmount = -bags,
                        resultingStock = invSilica?.stock ?: 0.0,
                        reason = "Packaging Order SB-${order.id}",
                        orderId = order.id
                    )
                )

                // Pembersih / Cleaner Foam
                val foamDeduct = item.quantity * 0.05
                inventoryDao.deductStock("inv_foam", foamDeduct)
                val invFoam = inventoryDao.getInventoryById("inv_foam")
                stockLogDao.insertLog(
                    StockLog(
                        inventoryId = "inv_foam",
                        inventoryName = invFoam?.name ?: "Cleaner Foam",
                        changeAmount = -foamDeduct,
                        resultingStock = invFoam?.stock ?: 0.0,
                        reason = "Cuci Sepatu SB-${order.id}",
                        orderId = order.id
                    )
                )
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: LaundryStatus) {
        val order = orderDao.getOrderById(orderId) ?: return
        val currentLog = try {
            JSONObject(order.statusLogJson)
        } catch (e: Exception) {
            JSONObject()
        }
        currentLog.put(newStatus.key, System.currentTimeMillis())

        val updated = order.copy(
            status = newStatus.key,
            statusLogJson = currentLog.toString(),
            isFollowedUpReady = if (newStatus == LaundryStatus.SIAP) order.isFollowedUpReady else false
        )
        orderDao.updateOrder(updated)
    }

    suspend fun updatePaymentStatus(orderId: String, isPaid: Boolean, paymentMethod: String) {
        val order = orderDao.getOrderById(orderId) ?: return
        val updated = order.copy(
            isPaid = isPaid,
            paymentMethod = paymentMethod,
            paidAt = if (isPaid) System.currentTimeMillis() else null
        )
        orderDao.updateOrder(updated)
    }

    suspend fun updateOrderEstimate(orderId: String, newDate: Long, note: String) {
        val order = orderDao.getOrderById(orderId) ?: return
        val updated = order.copy(
            estimatedDate = newDate,
            estimatedDateString = DateUtils.formatFullDate(newDate),
            estimatedNote = note
        )
        orderDao.updateOrder(updated)
    }

    suspend fun markOrderFollowedUp(orderId: String) {
        val order = orderDao.getOrderById(orderId) ?: return
        val updated = order.copy(
            isFollowedUpReady = true,
            reminderCount = order.reminderCount + 1
        )
        orderDao.updateOrder(updated)
    }

    suspend fun deleteOrder(orderId: String) {
        orderDao.deleteOrder(orderId)
    }

    suspend fun shiftAllActiveOrders(daysToAdd: Int, note: String) {
        val active = orderDao.getOrdersByDateKey(DateUtils.todayKey())
        val queueOrders = active.filter { LaundryStatus.fromKey(it.status).isQueue }
        val millisToAdd = daysToAdd * 86400000L
        for (o in queueOrders) {
            val newEst = o.estimatedDate + millisToAdd
            orderDao.updateOrder(
                o.copy(
                    estimatedDate = newEst,
                    estimatedDateString = DateUtils.formatFullDate(newEst),
                    estimatedNote = note
                )
            )
        }
    }

    suspend fun restockInventory(inventoryId: String, amount: Double, cost: Long) {
        inventoryDao.addStock(inventoryId, amount)
        val item = inventoryDao.getInventoryById(inventoryId) ?: return
        stockLogDao.insertLog(
            StockLog(
                inventoryId = inventoryId,
                inventoryName = item.name,
                changeAmount = amount,
                resultingStock = item.stock,
                reason = "Restock Masuk (+${amount} ${item.unit})",
                orderId = null
            )
        )
    }

    suspend fun saveInventoryItem(item: InventoryItem) {
        inventoryDao.insertInventory(item)
    }

    suspend fun deleteInventoryItem(item: InventoryItem) {
        inventoryDao.deleteInventory(item)
    }

    suspend fun saveService(service: Service) {
        serviceDao.insertService(service)
    }

    suspend fun deleteService(service: Service) {
        serviceDao.deleteService(service)
    }
}
