package com.minwoo.jangbogi

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.minwoo.jangbogi.data.JangbogiDatabase
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.UndoResult
import com.minwoo.jangbogi.data.UpdateItemResult
import com.minwoo.jangbogi.domain.Category
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryConsistencyTest {
    private lateinit var db: JangbogiDatabase
    private lateinit var repo: JangbogiRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            JangbogiDatabase::class.java
        ).build()
        repo = JangbogiRepository(db)
    }

    @After fun tearDown() { db.close() }

    @Test fun completedUndoRestoresOnlyItsOwnListAndCannotBeReused() = runBlocking {
        val listA = repo.createList("A")
        val listB = repo.createList("B")
        val dao = db.shoppingItemDao()
        val itemA = dao.insert(ShoppingItem(listId = listA, name = "eggs", isChecked = true, createdAt = 1))
        val itemB = dao.insert(ShoppingItem(listId = listB, name = "milk", isChecked = true, createdAt = 2))

        val undoA = requireNotNull(repo.clearCompleted(listA))
        val undoB = requireNotNull(repo.clearCompleted(listB))
        assertNull(dao.getById(itemA))
        assertNull(dao.getById(itemB))

        assertEquals(UndoResult.RESTORED, repo.undoClearCompleted(undoA))
        assertNotNull(dao.getById(itemA))
        assertNull(dao.getById(itemB))
        assertEquals(UndoResult.CONFLICT, repo.undoClearCompleted(undoA))
        assertEquals(UndoResult.RESTORED, repo.undoClearCompleted(undoB))
        assertNotNull(dao.getById(itemB))
    }

    @Test fun completedUndoDoesNotRestoreIntoDeletedList() = runBlocking {
        val listId = repo.createList("A")
        db.shoppingItemDao().insert(ShoppingItem(listId = listId, name = "eggs", isChecked = true, createdAt = 1))
        val undo = requireNotNull(repo.clearCompleted(listId))
        repo.deleteList(listId)

        assertEquals(UndoResult.MISSING_PARENT, repo.undoClearCompleted(undo))
    }

    @Test fun editingSharedPlanUpdatesSameNameItemsInOtherLists() = runBlocking {
        val listA = repo.createList("A")
        val listB = repo.createList("B")
        val dao = db.shoppingItemDao()
        val itemA = dao.insert(ShoppingItem(listId = listA, name = "coffee", createdAt = 1))
        val itemB = dao.insert(ShoppingItem(listId = listB, name = "coffee", createdAt = 2))

        assertEquals(
            UpdateItemResult.UPDATED,
            repo.updateItemAndPlan(itemA, "coffee", 2, Category.BEVERAGE, 1000, "market", 2000, 9, 3)
        )

        val updatedA = requireNotNull(dao.getById(itemA))
        val updatedB = requireNotNull(dao.getById(itemB))
        assertEquals(2, updatedA.quantity)
        assertEquals(1, updatedB.quantity)
        assertEquals(1000L, updatedB.plannedBuyAt)
        assertEquals("market", updatedB.preferredStore)
        assertEquals(2000L, updatedB.mustBuyBy)
        assertEquals(9, updatedB.stockUpMonth)
        assertEquals(3, updatedB.stockQuantity)
        assertEquals(1000L, db.itemPlanDao().getByName("coffee")?.plannedBuyAt)
    }
}
