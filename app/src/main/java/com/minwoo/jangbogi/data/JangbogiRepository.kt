package com.minwoo.jangbogi.data

import androidx.room.withTransaction
import com.minwoo.jangbogi.domain.Categorizer
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ListWithProgress
import com.minwoo.jangbogi.domain.QuantityParser
import com.minwoo.jangbogi.domain.Suggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JangbogiRepository(private val db: JangbogiDatabase) {

    private val listDao = db.shoppingListDao()
    private val itemDao = db.shoppingItemDao()
    private val historyDao = db.itemHistoryDao()

    // undo 캐시: 연산별 마지막 1건, 소비 시 비움 (VM 재생성과 무관하게 유지되도록 repo 보관)
    private var deletedItem: ShoppingItem? = null
    private var deletedList: Pair<ShoppingList, List<ShoppingItem>>? = null
    private var clearedCompleted: List<ShoppingItem>? = null

    private val whitespace = Regex("""\s+""")

    private fun normalizeName(name: String): String = name.trim().replace(whitespace, " ")

    fun observeListsWithProgress(): Flow<List<ListWithProgress>> = listDao.observeListsWithProgress()

    fun observeList(listId: Long): Flow<ShoppingList?> = listDao.observeList(listId)

    fun observeItems(listId: Long): Flow<List<ShoppingItem>> = itemDao.observeItems(listId)

    fun observeSuggestions(listId: Long, query: String): Flow<List<Suggestion>> =
        historyDao.observeSuggestions(listId, query)
            .map { rows -> rows.map { Suggestion(it.name, it.category) } }

    suspend fun createList(name: String): Long {
        val finalName = normalizeName(name).ifEmpty { "장보기 목록" }
        return listDao.insert(ShoppingList(name = finalName, createdAt = System.currentTimeMillis()))
    }

    suspend fun renameList(listId: Long, newName: String) {
        val finalName = normalizeName(newName)
        if (finalName.isNotEmpty()) listDao.rename(listId, finalName)
    }

    suspend fun deleteList(listId: Long) {
        db.withTransaction {
            val list = listDao.getById(listId) ?: return@withTransaction
            val items = itemDao.getAllOnce(listId)
            listDao.deleteById(listId)
            deletedList = list to items
        }
    }

    suspend fun undoDeleteList() {
        val (list, items) = deletedList ?: return
        deletedList = null
        db.withTransaction {
            listDao.insert(list)
            itemDao.insertAll(items)
        }
    }

    /** @return 실제로 추가/갱신됐으면 true (이름이 비면 false) */
    suspend fun addItem(listId: Long, rawInput: String): Boolean {
        val parsed = QuantityParser.parse(rawInput)
        val name = parsed.name
        if (name.isEmpty()) return false
        val now = System.currentTimeMillis()
        db.withTransaction {
            val history = historyDao.getByName(name)
            val category = history?.category ?: Categorizer.categorize(name)
            val existing = itemDao.findByName(listId, name)
            when {
                existing == null -> itemDao.insert(
                    ShoppingItem(
                        listId = listId,
                        name = name,
                        quantity = parsed.quantity,
                        category = category,
                        createdAt = now
                    )
                )
                !existing.isChecked -> itemDao.update(
                    existing.copy(quantity = existing.quantity + parsed.quantity)
                )
                else -> itemDao.update(
                    existing.copy(isChecked = false, checkedAt = null, quantity = parsed.quantity)
                )
            }
            if (historyDao.touch(name, now) == 0) {
                historyDao.insert(ItemHistory(name = name, category = category, useCount = 1, lastUsedAt = now))
            }
        }
        return true
    }

    suspend fun toggleItem(itemId: Long) {
        val item = itemDao.getById(itemId) ?: return
        val nowChecked = !item.isChecked
        itemDao.update(
            item.copy(
                isChecked = nowChecked,
                checkedAt = if (nowChecked) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun deleteItem(itemId: Long) {
        val item = itemDao.getById(itemId) ?: return
        itemDao.deleteById(itemId)
        deletedItem = item
    }

    suspend fun undoDeleteItem() {
        val item = deletedItem ?: return
        deletedItem = null
        itemDao.insert(item)
    }

    suspend fun updateItem(itemId: Long, name: String, quantity: Int, category: Category) {
        val finalName = normalizeName(name)
        if (finalName.isEmpty()) return
        db.withTransaction {
            val item = itemDao.getById(itemId) ?: return@withTransaction
            itemDao.update(
                item.copy(name = finalName, quantity = quantity.coerceIn(1, 99), category = category)
            )
            if (category != item.category) historyDao.updateCategory(finalName, category)
        }
    }

    suspend fun clearCompleted(listId: Long) {
        db.withTransaction {
            val completed = itemDao.getCompletedOnce(listId)
            if (completed.isEmpty()) return@withTransaction
            itemDao.deleteCompleted(listId)
            clearedCompleted = completed
        }
    }

    suspend fun undoClearCompleted() {
        val items = clearedCompleted ?: return
        clearedCompleted = null
        itemDao.insertAll(items)
    }
}
