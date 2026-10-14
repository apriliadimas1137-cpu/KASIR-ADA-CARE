package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class MaterialDeduction(
    val inventoryId: String,
    val amountPerPair: Double
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("inventoryId", inventoryId)
        put("amountPerPair", amountPerPair)
    }

    companion object {
        fun listToJson(list: List<MaterialDeduction>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun listFromJson(json: String): List<MaterialDeduction> {
            val list = mutableListOf<MaterialDeduction>()
            if (json.isBlank()) return list
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(MaterialDeduction(obj.getString("inventoryId"), obj.getDouble("amountPerPair")))
                }
            } catch (e: Exception) { }
            return list
        }
    }
}

@Entity(tableName = "services")
data class Service(
    @PrimaryKey
    val id: String,
    val name: String,
    val price: Long,
    val durationDays: Int = 1,
    val category: String = "CLEANING",
    val isActive: Boolean = true,
    val materialUsageJson: String = "[]",
    val sortOrder: Int = 0
) {
    val materialDeductions: List<MaterialDeduction>
        get() = MaterialDeduction.listFromJson(materialUsageJson)
}

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val stock: Double,
    val minStock: Double,
    val unit: String, // "ml", "pcs", "botol", "lembar"
    val costPerUnit: Long = 0L,
    val lastRestocked: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stock <= minStock
}

@Entity(tableName = "stock_logs")
data class StockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val inventoryId: String,
    val inventoryName: String,
    val changeAmount: Double,
    val resultingStock: Double,
    val reason: String, // "ORDER_USAGE", "RESTOCK", "CORRECTION"
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ShopSettings(
    val shopName: String = "Cuci Sepatu Express",
    val shopAddress: String = "Jl. Merdeka No. 45, Jakarta",
    val shopPhone: String = "081234567890",
    val dailyCapacity: Int = 12,
    val overloadDays: Int = 0,
    val autoQueueDelay: Boolean = true,
    val offDay: Int = -1, // -1: none, 0: Sun, 1: Mon, ...
    val qrisMerchantName: String = "SHOE CLEAN CARE",
    val qrisNmid: String = "ID1020030040050",
    val bankAccount: String = "BCA 8820192831 a/n Shoe Clean"
)
