package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.InventoryItem
import com.example.data.model.Order
import com.example.data.model.Service
import com.example.data.model.StockLog

@Database(
    entities = [
        Order::class,
        Service::class,
        InventoryItem::class,
        StockLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun stockLogDao(): StockLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shoe_cashier_pos.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
