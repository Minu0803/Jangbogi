package com.minwoo.jangbogi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    fun observeItems(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getById(itemId: Long): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND name = :name LIMIT 1")
    suspend fun findByName(listId: Long, name: String): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    suspend fun getAllOnce(listId: Long): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND isChecked = 1")
    suspend fun getCompletedOnce(listId: Long): List<ShoppingItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ShoppingItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ShoppingItem>)

    @Update
    suspend fun update(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND isChecked = 1")
    suspend fun deleteCompleted(listId: Long)
}
