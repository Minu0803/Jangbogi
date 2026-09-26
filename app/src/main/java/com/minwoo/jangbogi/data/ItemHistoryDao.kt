package com.minwoo.jangbogi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minwoo.jangbogi.domain.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemHistoryDao {

    @Query("SELECT * FROM item_history WHERE name = :name")
    suspend fun getByName(name: String): ItemHistory?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: ItemHistory): Long

    // minSdk 26의 SQLite에는 UPSERT 문법(3.24+)이 없어 UPDATE→미적중 시 INSERT 2단계로 처리
    @Query("UPDATE item_history SET useCount = useCount + 1, lastUsedAt = :now WHERE name = :name")
    suspend fun touch(name: String, now: Long): Int

    @Query("UPDATE item_history SET category = :category WHERE name = :name")
    suspend fun updateCategory(name: String, category: Category): Int

    @Query(
        """
        SELECT * FROM item_history
        WHERE (:query = '' OR name LIKE '%' || :query || '%')
          AND name NOT IN (
              SELECT name FROM shopping_items WHERE listId = :listId AND isChecked = 0 AND purchaseIntent = :intent
          )
        ORDER BY useCount DESC, lastUsedAt DESC
        LIMIT 10
        """
    )
    fun observeSuggestions(listId: Long, query: String, intent: com.minwoo.jangbogi.domain.PurchaseIntent): Flow<List<ItemHistory>>
}
