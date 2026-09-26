package com.minwoo.jangbogi.data

import androidx.room.withTransaction
import com.minwoo.jangbogi.domain.Categorizer
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ListWithProgress
import com.minwoo.jangbogi.domain.QuantityParser
import com.minwoo.jangbogi.domain.Suggestion
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.PurchaseIntentRules
import com.minwoo.jangbogi.domain.ActiveListSelector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class JangbogiRepository(private val db: JangbogiDatabase) {

    private val listDao = db.shoppingListDao()
    private val itemDao = db.shoppingItemDao()
    private val historyDao = db.itemHistoryDao()
    private val planDao = db.itemPlanDao()
    private val sessionDao = db.shoppingSessionDao()

    // undo 캐시: 연산별 마지막 1건, 소비 시 비움 (VM 재생성과 무관하게 유지되도록 repo 보관)
    private var deletedItem: ShoppingItem? = null
    private var deletedList: Pair<ShoppingList, List<ShoppingItem>>? = null
    private var clearedCompleted: List<ShoppingItem>? = null
    private var nextUndoId = 1L
    private val usedUndoIds = mutableSetOf<Long>()

    private val whitespace = Regex("""\s+""")

    private fun normalizeName(name: String): String = name.trim().replace(whitespace, " ")

    fun observeListsWithProgress(): Flow<List<ListWithProgress>> = listDao.observeListsWithProgress()

    fun observeList(listId: Long): Flow<ShoppingList?> = listDao.observeList(listId)

    suspend fun resolveActiveListId(): Long? = db.withTransaction {
        val saved = sessionDao.getActiveListId()
        val current = ActiveListSelector.choose(saved, listDao.observeListsWithProgress().first())
        if (saved != current) sessionDao.save(ShoppingSession(activeListId = current))
        current
    }

    suspend fun selectList(listId: Long): Boolean = db.withTransaction {
        if (listDao.getById(listId) == null) return@withTransaction false
        sessionDao.save(ShoppingSession(activeListId = listId))
        true
    }

    suspend fun addToNewList(rawInput: String, intent: PurchaseIntent, defaultName: String): Long? {
        if (QuantityParser.parse(rawInput).name.isEmpty()) return null
        return db.withTransaction {
            val listId = listDao.insert(
                ShoppingList(name = normalizeName(defaultName).ifEmpty { "내 장보기" }, createdAt = System.currentTimeMillis())
            )
            require(listId > 0)
            val result = addItem(listId, rawInput, intent)
            check(result == AddItemResult.Added)
            sessionDao.save(ShoppingSession(activeListId = listId))
            listId
        }
    }

    fun observeItems(listId: Long): Flow<List<ShoppingItem>> = itemDao.observeItems(listId)

    fun observeItemsForPlanning(): Flow<List<PlannedShoppingItem>> =
        combine(itemDao.observeAllWithListName(), planDao.observeAll()) { items, plans ->
            val listedNames = items.mapTo(mutableSetOf()) { it.item.name }
            val listedItems = items.map { row ->
                PlannedShoppingItem(item = row.item, listName = row.listName)
            }
            val unlistedPlans = plans
                .filterNot { it.name in listedNames }
                .map { plan ->
                    PlannedShoppingItem(
                        item = ShoppingItem(
                            id = plan.id,
                            listId = 0,
                            name = plan.name,
                            quantity = plan.quantity,
                            category = plan.category,
                            createdAt = 0,
                            plannedBuyAt = plan.plannedBuyAt,
                            preferredStore = plan.preferredStore,
                            mustBuyBy = plan.mustBuyBy,
                            stockUpMonth = plan.stockUpMonth,
                            stockQuantity = plan.stockQuantity
                        ),
                        listName = ""
                    )
                }
            // 계획만 저장해 둔 품목도 장보기 목록에서 삭제되지 않도록 함께 보여준다.
            listedItems + unlistedPlans
        }

    fun observeSuggestions(listId: Long, query: String, intent: PurchaseIntent = PurchaseIntent.BUY): Flow<List<Suggestion>> =
        historyDao.observeSuggestions(listId, query, intent)
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

    suspend fun addItem(listId: Long, rawInput: String, intent: PurchaseIntent = PurchaseIntent.BUY): AddItemResult {
        val parsed = QuantityParser.parse(rawInput)
        val name = parsed.name
        if (name.isEmpty()) return AddItemResult.EmptyInput
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val history = historyDao.getByName(name)
            val plan = planDao.getByName(name)
            val category = history?.category ?: Categorizer.categorize(name)
            val existing = itemDao.findByName(listId, name)
            val result = when {
                existing == null -> itemDao.insert(
                    ShoppingItem(
                        listId = listId,
                        name = name,
                        quantity = parsed.quantity,
                        category = category,
                        createdAt = now,
                        plannedBuyAt = plan?.plannedBuyAt,
                        preferredStore = plan?.preferredStore,
                        mustBuyBy = plan?.mustBuyBy,
                        stockUpMonth = plan?.stockUpMonth,
                        stockQuantity = plan?.stockQuantity ?: 0,
                        purchaseIntent = intent
                    )
                ).let { AddItemResult.Added }
                existing.purchaseIntent != intent -> AddItemResult.OtherIntent(existing.id, existing.purchaseIntent)
                !existing.isChecked && existing.quantity + parsed.quantity > 99 -> AddItemResult.QuantityLimit
                !existing.isChecked -> {
                    itemDao.update(existing.copy(quantity = existing.quantity + parsed.quantity))
                    AddItemResult.Merged
                }
                else -> {
                    itemDao.update(existing.copy(
                        isChecked = false,
                        checkedAt = null,
                        quantity = parsed.quantity,
                        plannedBuyAt = plan?.plannedBuyAt ?: existing.plannedBuyAt,
                        preferredStore = plan?.preferredStore ?: existing.preferredStore,
                        mustBuyBy = plan?.mustBuyBy ?: existing.mustBuyBy,
                        stockUpMonth = plan?.stockUpMonth ?: existing.stockUpMonth,
                        stockQuantity = plan?.stockQuantity ?: existing.stockQuantity
                    ))
                    AddItemResult.Reopened
                }
            }
            if (result !is AddItemResult.OtherIntent && result != AddItemResult.QuantityLimit && historyDao.touch(name, now) == 0) {
                historyDao.insert(ItemHistory(name = name, category = category, useCount = 1, lastUsedAt = now))
            }
            result
        }
    }

    suspend fun toggleItem(itemId: Long) {
        val item = itemDao.getById(itemId) ?: return
        if (item.purchaseIntent == PurchaseIntent.CONSIDER) return
        itemDao.update(PurchaseIntentRules.toggle(item, System.currentTimeMillis()))
    }

    suspend fun moveItem(itemId: Long, intent: PurchaseIntent): ItemMutationResult = db.withTransaction {
        val before = itemDao.getById(itemId) ?: return@withTransaction ItemMutationResult.Missing
        if (before.purchaseIntent == intent) return@withTransaction ItemMutationResult.NoChange
        val after = PurchaseIntentRules.move(before, intent)
        itemDao.update(after)
        ItemMutationResult.Applied(UndoToken(nextUndoId++, before, after))
    }

    suspend fun deleteItemWithUndo(itemId: Long): ItemMutationResult = db.withTransaction {
        val before = itemDao.getById(itemId) ?: return@withTransaction ItemMutationResult.Missing
        itemDao.deleteById(itemId)
        ItemMutationResult.Applied(UndoToken(nextUndoId++, before, null))
    }

    suspend fun undoMutation(token: UndoToken): UndoResult = db.withTransaction {
        if (token.id in usedUndoIds) return@withTransaction UndoResult.CONFLICT
        if (listDao.getById(token.before.listId) == null) return@withTransaction UndoResult.MISSING_PARENT
        val current = itemDao.getById(token.before.id)
        if (current != token.after) return@withTransaction UndoResult.CONFLICT
        usedUndoIds += token.id
        if (current == null) itemDao.insert(token.before) else itemDao.update(token.before)
        UndoResult.RESTORED
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

    suspend fun updateShoppingPlan(
        name: String,
        quantity: Int,
        category: Category,
        plannedBuyAt: Long?,
        preferredStore: String,
        mustBuyBy: Long?,
        stockUpMonth: Int?,
        stockQuantity: Int
    ) {
        val finalName = normalizeName(name)
        if (finalName.isEmpty()) return
        val finalStore = preferredStore.trim().ifEmpty { null }
        val finalStock = stockQuantity.coerceIn(0, 99)
        db.withTransaction {
            itemDao.updatePlanFieldsByName(finalName, plannedBuyAt, finalStore, mustBuyBy, stockUpMonth, finalStock)
            planDao.save(ItemPlan(0, finalName, quantity.coerceIn(1, 99), category, plannedBuyAt, finalStore, mustBuyBy, stockUpMonth, finalStock))
        }
    }

    suspend fun updateItemAndPlan(
        itemId: Long,
        name: String,
        quantity: Int,
        category: Category,
        plannedBuyAt: Long?,
        preferredStore: String,
        mustBuyBy: Long?,
        stockUpMonth: Int?,
        stockQuantity: Int
    ) {
        val finalName = normalizeName(name)
        if (finalName.isEmpty()) return
        db.withTransaction {
            val item = itemDao.getById(itemId) ?: return@withTransaction
            itemDao.update(
                item.copy(
                    name = finalName,
                    quantity = quantity.coerceIn(1, 99),
                    category = category,
                    plannedBuyAt = plannedBuyAt,
                    preferredStore = preferredStore.trim().ifEmpty { null },
                    mustBuyBy = mustBuyBy,
                    stockUpMonth = stockUpMonth,
                    stockQuantity = stockQuantity.coerceIn(0, 99)
                )
            )
            val finalStore = preferredStore.trim().ifEmpty { null }
            val finalStock = stockQuantity.coerceIn(0, 99)
            planDao.save(ItemPlan(0, finalName, quantity.coerceIn(1, 99), category, plannedBuyAt, finalStore, mustBuyBy, stockUpMonth, finalStock))
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
