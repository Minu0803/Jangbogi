package com.minwoo.jangbogi

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.minwoo.jangbogi.data.*
import com.minwoo.jangbogi.domain.*
import com.minwoo.jangbogi.ui.viewmodel.ListViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class DecisionViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var db: JangbogiDatabase
    private lateinit var repo: JangbogiRepository
    private class CountingRandom(private val buy: Boolean) : Random() {
        var calls = 0
        override fun nextBits(bitCount: Int): Int { calls++; return if (calls == 1 && buy) 1 else 0 }
    }
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, JangbogiDatabase::class.java).build()
        repo = JangbogiRepository(db)
    }
    @After fun close() { db.close() }
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun awaitPhase(vm: ListViewModel, phase: DecisionPhase) {
        val until = System.currentTimeMillis() + 5_000
        while (vm.decisionSession.value?.phase != phase && System.currentTimeMillis() < until) {
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(20)
        }
        assertEquals(phase, vm.decisionSession.value?.phase)
    }

    @Test fun repeatedClicksAndCallbacksCannotRerollOrDeleteRestoredItem() = runBlocking {
        for (buy in listOf(true, false)) {
            val list = repo.createList("세션 검증")
            val id = db.shoppingItemDao().insert(ShoppingItem(listId = list, name = "이어폰", createdAt = 1, purchaseIntent = PurchaseIntent.CONSIDER))
            val before = requireNotNull(db.shoppingItemDao().getById(id))
            val random = CountingRandom(buy)
            val vm = ListViewModel(repo, list, random)
            main {
                vm.openDecision(before); vm.openDecision(before)
                vm.startDecision(); vm.startDecision()
                assertEquals(2, random.calls)
                assertEquals(if (buy) DecisionOutcome.BUY else DecisionOutcome.SKIP, vm.decisionSession.value?.spin?.outcome)
                assertNull(vm.dismissDecision())
                vm.finishDecisionAnimation(1); vm.finishDecisionAnimation(1)
            }
            awaitPhase(vm, DecisionPhase.RESULT)
            var dismissal: com.minwoo.jangbogi.ui.viewmodel.DecisionDismissal? = null
            main { dismissal = vm.dismissDecision(); assertNull(vm.dismissDecision()) }
            assertEquals(UndoResult.RESTORED, repo.undoMutation(requireNotNull(dismissal).undoToken))
            main { vm.finishDecisionAnimation(1); vm.startDecision() }
            assertEquals(before, db.shoppingItemDao().getById(id))
        }
    }

    @Test fun staleItemShowsErrorAndRetryKeepsOriginalResult() = runBlocking {
        val list = repo.createList("충돌 검증")
        val dao = db.shoppingItemDao()
        val id = dao.insert(ShoppingItem(listId = list, name = "보관 물건", createdAt = 1, purchaseIntent = PurchaseIntent.CONSIDER))
        val before = requireNotNull(dao.getById(id))
        val random = CountingRandom(false)
        val vm = ListViewModel(repo, list, random)
        main { vm.openDecision(before); vm.startDecision() }
        val chosen = vm.decisionSession.value?.spin
        dao.update(before.copy(quantity = 3))
        main { vm.finishDecisionAnimation(1) }
        awaitPhase(vm, DecisionPhase.ERROR)
        main { vm.retryDecision() }
        awaitPhase(vm, DecisionPhase.ERROR)
        assertEquals(chosen, vm.decisionSession.value?.spin)
        assertEquals(2, random.calls)
        assertEquals(3, dao.getById(id)?.quantity)
        main { assertNull(vm.dismissDecision()) }
    }
    @Test fun persistenceExceptionNeverReportsSuccessAndRetryDoesNotReroll() = runBlocking {
        val list = repo.createList("저장 실패 검증")
        val id = db.shoppingItemDao().insert(ShoppingItem(listId = list, name = "저장할 물건",
            createdAt = 1, purchaseIntent = PurchaseIntent.CONSIDER))
        val before = requireNotNull(db.shoppingItemDao().getById(id))
        val random = CountingRandom(true)
        val vm = ListViewModel(repo, list, random)
        main { vm.openDecision(before); vm.startDecision() }
        val chosen = vm.decisionSession.value?.spin
        // Room may reopen after close; a SQLite trigger reliably aborts only this fixture write.
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_decision BEFORE UPDATE ON shopping_items BEGIN SELECT RAISE(ABORT, 'fixture write failed'); END"
        )
        main { vm.finishDecisionAnimation(1) }
        awaitPhase(vm, DecisionPhase.ERROR)
        assertTrue(vm.decisionSession.value?.error?.startsWith("저장") == true)
        assertNull(vm.decisionSession.value?.undoToken)
        assertEquals(before, db.shoppingItemDao().getById(id))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_decision")
        main { vm.retryDecision() }
        awaitPhase(vm, DecisionPhase.RESULT)
        assertEquals(chosen, vm.decisionSession.value?.spin)
        assertEquals(2, random.calls)
        assertEquals(before.copy(purchaseIntent = PurchaseIntent.BUY), db.shoppingItemDao().getById(id))
        var dismissal: com.minwoo.jangbogi.ui.viewmodel.DecisionDismissal? = null
        main { dismissal = vm.dismissDecision() }
        assertEquals(UndoResult.RESTORED, repo.undoMutation(requireNotNull(dismissal).undoToken))
        assertEquals(before, db.shoppingItemDao().getById(id))
    }

}
