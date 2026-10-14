package com.example.data.local

import androidx.room.*
import com.example.data.model.InventoryItem
import com.example.data.model.StockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY name ASC")
    fun getAllInventory(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory WHERE stock <= minStock")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory WHERE id = :id LIMIT 1")
    suspend fun getInventoryById(id: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventories(items: List<InventoryItem>)

    @Update
    suspend fun updateInventory(item: InventoryItem)

    @Query("UPDATE inventory SET stock = MAX(0.0, stock - :amount) WHERE id = :id")
    suspend fun deductStock(id: String, amount: Double)

    @Query("UPDATE inventory SET stock = stock + :amount, lastRestocked = :timestamp WHERE id = :id")
    suspend fun addStock(id: String, amount: Double, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInventory(item: InventoryItem)

    @Query("SELECT COUNT(*) FROM inventory")
    suspend fun getCount(): Int
}

@Dao
interface StockLogDao {
    @Query("SELECT * FROM stock_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<StockLog>>

    @Insert
    suspend fun insertLog(log: StockLog)
}
