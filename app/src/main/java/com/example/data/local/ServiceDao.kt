package com.example.data.local

import androidx.room.*
import com.example.data.model.Service
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY sortOrder ASC, name ASC")
    fun getAllServices(): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getActiveServices(): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceById(id: String): Service?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<Service>)

    @Update
    suspend fun updateService(service: Service)

    @Delete
    suspend fun deleteService(service: Service)

    @Query("SELECT COUNT(*) FROM services")
    suspend fun getCount(): Int
}
