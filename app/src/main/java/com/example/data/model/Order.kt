package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class OrderItem(
    val serviceId: String,
    val serviceName: String,
    val price: Long,
    val durationDays: Int,
    val quantity: Int = 1,
    val shoeNotes: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("serviceId", serviceId)
            put("serviceName", serviceName)
            put("price", price)
            put("durationDays", durationDays)
            put("quantity", quantity)
            put("shoeNotes", shoeNotes)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): OrderItem {
            return OrderItem(
                serviceId = obj.optString("serviceId"),
                serviceName = obj.optString("serviceName", "Layanan"),
                price = obj.optLong("price", 0L),
                durationDays = obj.optInt("durationDays", 1),
                quantity = obj.optInt("quantity", 1),
                shoeNotes = obj.optString("shoeNotes", "")
            )
        }

        fun listToJson(list: List<OrderItem>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String): List<OrderItem> {
            val list = mutableListOf<OrderItem>()
            if (jsonStr.isBlank()) return list
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Return empty if parsing error
            }
            return list
        }
    }
}

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    val id: String, // e.g. "SB-7X9K"
    val customerName: String,
    val customerPhone: String,
    val status: String = LaundryStatus.DITERIMA.key,
    val createdAt: Long = System.currentTimeMillis(),
    val dateKey: String, // "yyyy-MM-dd"
    val queueNumber: Int = 1,
    val estimatedDate: Long,
    val estimatedDateString: String,
    val estimatedNote: String = "",
    val subtotalAmount: Long = 0L,
    val discountAmount: Long = 0L,
    val discountName: String = "",
    val totalAmount: Long,
    val pairsCount: Int,
    val isPaid: Boolean = false,
    val paymentMethod: String = PaymentMethod.CASH.id,
    val paidAt: Long? = null,
    val notes: String = "",
    val itemsJson: String = "",
    val statusLogJson: String = "{}",
    val isFollowedUpReady: Boolean = false,
    val reminderCount: Int = 0
) {
    val items: List<OrderItem>
        get() = OrderItem.listFromJson(itemsJson)

    val currentStatus: LaundryStatus
        get() = LaundryStatus.fromKey(status)
}
