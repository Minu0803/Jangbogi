package com.minwoo.jangbogi

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.minwoo.jangbogi.data.ItemMutationResult
import com.minwoo.jangbogi.data.JangbogiDatabase
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.UndoResult
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.DecisionOutcome
import com.minwoo.jangbogi.domain.PurchaseIntent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DecisionRepositoryTest {
    private lateinit var db: JangbogiDatabase
    private lateinit var repo: JangbogiRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, JangbogiDatabase::class.java).build()
        repo = JangbogiRepository(db)
    }
    @After fun close() = db.close()

    @Test fun buyPreservesFieldsAndUndoRestoresOnce() = runBlocking {
        val list = repo.createList("A")
        val otherList = repo.createList("B")
        val dao = db.shoppingItemDao()
        val id = dao.insert(ShoppingItem(listId = list, name = "긴 물건", quantity = 4, category = Category.ETC,
            createdAt = 100, plannedBuyAt = 200, preferredStore = "가게", mustBuyBy = 300,
            stockUpMonth = 9, stockQuantity = 2, purchaseIntent = PurchaseIntent.CONSIDER))
        val otherId = dao.insert(ShoppingItem(listId = otherList, name = "다른 물건", createdAt = 7))
        val before = requireNotNull(dao.getById(id))
        val other = dao.getById(otherId)
        val applied = repo.applyDecision(before, DecisionOutcome.BUY) as ItemMutationResult.Applied
        assertEquals(before.copy(purchaseIntent = PurchaseIntent.BUY), dao.getById(id))
        assertEquals(other, dao.getById(otherId))
        assertEquals(ItemMutationResult.NoChange, repo.applyDecision(before, DecisionOutcome.SKIP))
        assertEquals(UndoResult.RESTORED, repo.undoMutation(applied.undoToken))
        assertEquals(before, dao.getById(id))
        assertEquals(UndoResult.CONFLICT, repo.undoMutation(applied.undoToken))
    }

    @Test fun skipDeletesOnlyExpectedItemAndUndoRestores() = runBlocking {
        val list = repo.createList("A")
        val dao = db.shoppingItemDao()
        val id = dao.insert(ShoppingItem(listId = list, name = "고민", createdAt = 1, purchaseIntent = PurchaseIntent.CONSIDER))
        val otherId = dao.insert(ShoppingItem(listId = list, name = "그대로", createdAt = 2))
        val before = requireNotNull(dao.getById(id))
        val applied = repo.applyDecision(before, DecisionOutcome.SKIP) as ItemMutationResult.Applied
        assertNull(dao.getById(id))
        assertEquals("그대로", dao.getById(otherId)?.name)
        assertEquals(UndoResult.RESTORED, repo.undoMutation(applied.undoToken))
        assertEquals(before, dao.getById(id))
        assertEquals(UndoResult.CONFLICT, repo.undoMutation(applied.undoToken))
    }

    @Test fun editedOrMissingSnapshotCannotChangeItem() = runBlocking {
        val list = repo.createList("A")
        val dao = db.shoppingItemDao()
        val id = dao.insert(ShoppingItem(listId = list, name = "고민", createdAt = 1, purchaseIntent = PurchaseIntent.CONSIDER))
        val before = requireNotNull(dao.getById(id))
        dao.update(before.copy(quantity = 2))
        assertEquals(ItemMutationResult.NoChange, repo.applyDecision(before, DecisionOutcome.SKIP))
        assertEquals(2, dao.getById(id)?.quantity)
        dao.deleteById(id)
        assertEquals(ItemMutationResult.Missing, repo.applyDecision(before, DecisionOutcome.BUY))
    }
}
