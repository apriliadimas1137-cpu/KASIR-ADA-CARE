package com.example.data.local

import androidx.room.*
import com.example.data.model.Order
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status IN ('diterima', 'dicuci', 'dikeringkan', 'finishing') ORDER BY estimatedDate ASC, createdAt ASC")
    fun getActiveQueueOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): Order?

    @Query("SELECT * FROM orders WHERE dateKey = :dateKey")
    suspend fun getOrdersByDateKey(dateKey: String): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: String)

    @Query("SELECT COUNT(*) FROM orders WHERE status IN ('diterima', 'dicuci', 'dikeringkan', 'finishing')")
    fun getActiveQueueCount(): Flow<Int>

    @Query("SELECT SUM(pairsCount) FROM orders WHERE status IN ('diterima', 'dicuci', 'dikeringkan', 'finishing')")
    fun getActivePairsCount(): Flow<Int?>

    @Query("SELECT * FROM orders WHERE createdAt >= :startTime ORDER BY createdAt ASC")
    fun getOrdersSince(startTime: Long): Flow<List<Order>>
}
