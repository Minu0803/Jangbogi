package com.minwoo.jangbogi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.minwoo.jangbogi.domain.Category
import kotlinx.coroutines.flow.Flow

data class PlannedShoppingItem(
    @androidx.room.Embedded val item: ShoppingItem,
    val listName: String
) {
    val stableKey: String get() = if (item.listId == 0L) "plan:${item.id}" else "item:${item.id}"
}

data class ShoppingItemWithListName(
    @androidx.room.Embedded val item: ShoppingItem,
    val listName: String
)

@Dao
interface ShoppingItemDao {

    @Query(
        """
        SELECT shopping_items.*, shopping_lists.name AS listName
        FROM shopping_items
        INNER JOIN shopping_lists ON shopping_lists.id = shopping_items.listId
        ORDER BY shopping_lists.createdAt DESC, shopping_items.createdAt, shopping_items.name COLLATE NOCASE
        """
    )
    fun observeAllWithListName(): Flow<List<ShoppingItemWithListName>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    fun observeItems(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getById(itemId: Long): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND name = :name LIMIT 1")
    suspend fun findByName(listId: Long, name: String): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    suspend fun getAllOnce(listId: Long): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND name = :name AND id != :exceptId LIMIT 1")
    suspend fun findOtherByName(listId: Long, name: String, exceptId: Long): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE listId = :listId AND isChecked = 1")
    suspend fun getCompletedOnce(listId: Long): List<ShoppingItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ShoppingItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ShoppingItem>)

    @Update
    suspend fun update(item: ShoppingItem)

    @Query("UPDATE shopping_items SET quantity = :quantity, category = :category, plannedBuyAt = :plannedBuyAt, preferredStore = :preferredStore, mustBuyBy = :mustBuyBy, stockUpMonth = :stockUpMonth, stockQuantity = :stockQuantity WHERE name = :name")
    suspend fun updatePlanByName(name: String, quantity: Int, category: Category, plannedBuyAt: Long?, preferredStore: String?, mustBuyBy: Long?, stockUpMonth: Int?, stockQuantity: Int)

    @Query("UPDATE shopping_items SET plannedBuyAt = :plannedBuyAt, preferredStore = :preferredStore, mustBuyBy = :mustBuyBy, stockUpMonth = :stockUpMonth, stockQuantity = :stockQuantity WHERE name = :name")
    suspend fun updatePlanFieldsByName(name: String, plannedBuyAt: Long?, preferredStore: String?, mustBuyBy: Long?, stockUpMonth: Int?, stockQuantity: Int)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND isChecked = 1")
    suspend fun deleteCompleted(listId: Long)
}
