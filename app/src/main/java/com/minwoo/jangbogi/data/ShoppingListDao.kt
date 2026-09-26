package com.minwoo.jangbogi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minwoo.jangbogi.domain.ListWithProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query(
        """
        SELECT l.*,
               COALESCE(SUM(CASE WHEN i.purchaseIntent = 'BUY' THEN 1 ELSE 0 END), 0) AS totalCount,
               COALESCE(SUM(CASE WHEN i.purchaseIntent = 'BUY' AND i.isChecked THEN 1 ELSE 0 END), 0) AS checkedCount,
               COALESCE(SUM(CASE WHEN i.purchaseIntent = 'CONSIDER' THEN 1 ELSE 0 END), 0) AS consideringCount
        FROM shopping_lists AS l
        LEFT JOIN shopping_items AS i ON i.listId = l.id
        GROUP BY l.id
        ORDER BY l.createdAt DESC
        """
    )
    fun observeListsWithProgress(): Flow<List<ListWithProgress>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    fun observeList(listId: Long): Flow<ShoppingList?>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getById(listId: Long): ShoppingList?

    @Query("SELECT id FROM shopping_lists ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun mostRecentId(): Long?

    @Query("SELECT l.id FROM shopping_lists l WHERE EXISTS (SELECT 1 FROM shopping_items i WHERE i.listId = l.id AND i.purchaseIntent = 'BUY' AND i.isChecked = 0) ORDER BY l.createdAt DESC, l.id DESC LIMIT 1")
    suspend fun mostRecentActiveId(): Long?

    // IGNORE: undo 재삽입 시 id가 이미 재사용된 극단 케이스에서 크래시 대신 무시
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(list: ShoppingList): Long

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :listId")
    suspend fun rename(listId: Long, name: String)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteById(listId: Long)
}
