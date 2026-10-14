package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.LaundryRepository
import com.example.ui.components.DailyStat
import com.example.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class PosDraft(
    val customerName: String = "",
    val customerPhone: String = "",
    val selectedLines: List<OrderItem> = emptyList(),
    val isPaid: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val manualEstimatedDate: Long? = null,
    val appliedPromo: DiscountPromo? = null,
    val customDiscountAmount: Long = 0L,
    val customDiscountName: String = ""
) {
    val subtotalAmount: Long
        get() = selectedLines.sumOf { it.price * it.quantity }

    val discountAmount: Long
        get() {
            if (appliedPromo != null) {
                return appliedPromo.calculateDiscount(subtotalAmount)
            }
            return customDiscountAmount.coerceAtMost(subtotalAmount)
        }

    val discountName: String
        get() {
            if (appliedPromo != null) return appliedPromo.title
            return customDiscountName
        }

    val totalAmount: Long
        get() = maxOf(0L, subtotalAmount - discountAmount)

    val totalPairs: Int
        get() = selectedLines.sumOf { it.quantity }

    val isValid: Boolean
        get() = customerName.isNotBlank() && selectedLines.isNotEmpty()
}

data class PosReportKpi(
    val todayRevenue: Long = 0L,
    val totalRevenue: Long = 0L,
    val activeQueueCount: Int = 0,
    val activePairsCount: Int = 0,
    val readyOrdersCount: Int = 0,
    val unpaidOrdersCount: Int = 0,
    val unpaidAmount: Long = 0L
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = LaundryRepository(database, application, viewModelScope)

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeQueue: StateFlow<List<Order>> = repository.activeQueueOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val services: StateFlow<List<Service>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pelanggan terdaftar di database (nama & nomor HP unik)
    val registeredCustomers: StateFlow<List<Pair<String, String>>> = repository.allOrders
        .map { list ->
            list.filter { it.customerName.isNotBlank() && it.customerPhone.isNotBlank() }
                .groupBy { it.customerPhone }
                .map { (_, orders) -> Pair(orders.first().customerName, orders.first().customerPhone) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<InventoryItem>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockLogs: StateFlow<List<StockLog>> = repository.recentStockLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shopSettings: StateFlow<ShopSettings> = repository.shopSettings

    // POS Screen Draft State
    private val _draft = MutableStateFlow(PosDraft())
    val draft: StateFlow<PosDraft> = _draft.asStateFlow()

    // Active Selected Order for Detail / Receipt
    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    fun updateDraftCustomer(name: String, phone: String) {
        _draft.update { it.copy(customerName = name, customerPhone = phone) }
    }

    fun updateDraftNotes(notes: String) {
        _draft.update { it.copy(notes = notes) }
    }

    fun updateDraftPayment(isPaid: Boolean, method: PaymentMethod) {
        _draft.update { it.copy(isPaid = isPaid, paymentMethod = method) }
    }

    fun setDraftManualEst(est: Long?) {
        _draft.update { it.copy(manualEstimatedDate = est) }
    }

    fun addServiceToDraft(service: Service) {
        _draft.update { current ->
            val existingIdx = current.selectedLines.indexOfFirst { it.serviceId == service.id && it.shoeNotes.isBlank() }
            val newLines = current.selectedLines.toMutableList()
            if (existingIdx >= 0) {
                val existing = newLines[existingIdx]
                newLines[existingIdx] = existing.copy(quantity = existing.quantity + 1)
            } else {
                newLines.add(
                    OrderItem(
                        serviceId = service.id,
                        serviceName = service.name,
                        price = service.price,
                        durationDays = service.durationDays,
                        quantity = 1,
                        shoeNotes = ""
                    )
                )
            }
            current.copy(selectedLines = newLines)
        }
    }

    fun updateItemQty(index: Int, delta: Int) {
        _draft.update { current ->
            val newLines = current.selectedLines.toMutableList()
            if (index in newLines.indices) {
                val item = newLines[index]
                val newQty = item.quantity + delta
                if (newQty <= 0) {
                    newLines.removeAt(index)
                } else {
                    newLines[index] = item.copy(quantity = newQty)
                }
            }
            current.copy(selectedLines = newLines)
        }
    }

    fun updateItemNotes(index: Int, shoeNotes: String) {
        _draft.update { current ->
            val newLines = current.selectedLines.toMutableList()
            if (index in newLines.indices) {
                newLines[index] = newLines[index].copy(shoeNotes = shoeNotes)
            }
            current.copy(selectedLines = newLines)
        }
    }

    fun applyPromo(promo: DiscountPromo?) {
        _draft.update { it.copy(appliedPromo = promo, customDiscountAmount = 0L, customDiscountName = "") }
    }

    fun applyCustomDiscount(name: String, amount: Long) {
        _draft.update { it.copy(appliedPromo = null, customDiscountAmount = amount, customDiscountName = name) }
    }

    fun clearDiscount() {
        _draft.update { it.copy(appliedPromo = null, customDiscountAmount = 0L, customDiscountName = "") }
    }

    fun clearDraft() {
        _draft.value = PosDraft()
    }

    fun submitDraftOrder(onSuccess: (Order) -> Unit) {
        val current = _draft.value
        if (!current.isValid) return

        viewModelScope.launch {
            val created = repository.createOrder(
                customerName = current.customerName,
                customerPhone = current.customerPhone,
                items = current.selectedLines,
                isPaid = current.isPaid,
                paymentMethod = current.paymentMethod.id,
                manualEstimatedDate = current.manualEstimatedDate,
                notes = current.notes,
                discountAmount = current.discountAmount,
                discountName = current.discountName
            )
            clearDraft()
            _selectedOrder.value = created
            onSuccess(created)
        }
    }

    fun selectOrder(order: Order?) {
        _selectedOrder.value = order
    }

    fun advanceOrderStatus(orderId: String, newStatus: LaundryStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            // Update selected order if open
            if (_selectedOrder.value?.id == orderId) {
                _selectedOrder.update { it?.copy(status = newStatus.key) }
            }
        }
    }

    fun updateOrderPayment(orderId: String, isPaid: Boolean, paymentMethod: String = PaymentMethod.CASH.id) {
        viewModelScope.launch {
            repository.updatePaymentStatus(orderId, isPaid, paymentMethod)
            if (_selectedOrder.value?.id == orderId) {
                _selectedOrder.update { it?.copy(isPaid = isPaid, paymentMethod = paymentMethod) }
            }
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            if (_selectedOrder.value?.id == orderId) {
                _selectedOrder.value = null
            }
        }
    }

    fun shiftQueueOrders(days: Int, note: String) {
        viewModelScope.launch {
            repository.shiftAllActiveOrders(days, note)
        }
    }

    fun restock(inventoryId: String, amount: Double, cost: Long) {
        viewModelScope.launch {
            repository.restockInventory(inventoryId, amount, cost)
        }
    }

    fun saveInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.saveInventoryItem(item)
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    fun saveService(service: Service) {
        viewModelScope.launch {
            repository.saveService(service)
        }
    }

    fun deleteService(service: Service) {
        viewModelScope.launch {
            repository.deleteService(service)
        }
    }

    fun saveShopSettings(settings: ShopSettings) {
        viewModelScope.launch {
            repository.saveShopSettings(settings)
        }
    }

    // Reports & KPI Calculation
    fun calculateKpi(allOrders: List<Order>): PosReportKpi {
        val todayKey = DateUtils.todayKey()
        var todayRev = 0L
        var totalRev = 0L
        var activeQ = 0
        var activePairs = 0
        var readyCount = 0
        var unpaidCount = 0
        var unpaidAmount = 0L

        for (o in allOrders) {
            if (o.isPaid) {
                totalRev += o.totalAmount
                if (o.dateKey == todayKey) {
                    todayRev += o.totalAmount
                }
            } else {
                unpaidCount++
                unpaidAmount += o.totalAmount
            }

            if (o.currentStatus.isQueue) {
                activeQ++
                activePairs += o.pairsCount
            } else if (o.currentStatus == LaundryStatus.SIAP) {
                readyCount++
            }
        }

        return PosReportKpi(
            todayRevenue = todayRev,
            totalRevenue = totalRev,
            activeQueueCount = activeQ,
            activePairsCount = activePairs,
            readyOrdersCount = readyCount,
            unpaidOrdersCount = unpaidCount,
            unpaidAmount = unpaidAmount
        )
    }

    fun calculate7DaysStats(allOrders: List<Order>): List<DailyStat> {
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEE", Locale("id", "ID"))
        val cal = Calendar.getInstance()

        val last7Days = mutableListOf<DailyStat>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val key = sdfKey.format(c.time)
            val dayName = sdfDay.format(c.time)

            val matching = allOrders.filter { it.dateKey == key }
            val rev = matching.filter { it.isPaid }.sumOf { it.totalAmount }
            val pairs = matching.sumOf { it.pairsCount }

            last7Days.add(DailyStat(dateLabel = key, dayName = dayName, revenue = rev, pairs = pairs))
        }
        return last7Days
    }

    fun calculatePaymentBreakdown(allOrders: List<Order>): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for (o in allOrders) {
            if (o.isPaid) {
                val methodObj = PaymentMethod.fromId(o.paymentMethod)
                val key = methodObj.displayName
                map[key] = (map[key] ?: 0L) + o.totalAmount
            }
        }
        return map
    }
}
