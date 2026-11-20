package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ItemStatus
import com.example.data.model.PantryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items WHERE status = 'ACTIVE' ORDER BY expiryDate ASC, name ASC")
    fun observeActiveItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE status != 'ACTIVE' ORDER BY expiryDate DESC, name ASC")
    fun observeHistoryItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items ORDER BY expiryDate ASC")
    fun observeAllItems(): Flow<List<PantryItem>>

    @Query("SELECT COUNT(*) FROM pantry_items")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PantryItem>)

    @Update
    suspend fun updateItem(item: PantryItem)

    @Query("UPDATE pantry_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ItemStatus)

    @Query("UPDATE pantry_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pantry_items WHERE status != 'ACTIVE'")
    suspend fun clearHistory()

    @Query("DELETE FROM pantry_items")
    suspend fun clearAll()
}
