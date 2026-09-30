package com.minwoo.jangbogi

import android.graphics.Bitmap
import android.content.ContentValues
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.minwoo.jangbogi.data.*
import com.minwoo.jangbogi.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*

class DecisionAppFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var db: JangbogiDatabase
    private lateinit var repo: JangbogiRepository
    private lateinit var before: ShoppingItem
    private var listId = 0L
    private var scenario: ActivityScenario<MainActivity>? = null
    private val captureMode get() = InstrumentationRegistry.getArguments().getString("captureMode") ?: "light"

    private val animationsDisabled get() = InstrumentationRegistry.getArguments().getString("animationZero") == "true"

    @Before fun seed() = runBlocking {
        db = Room.databaseBuilder(instrumentation.targetContext, JangbogiDatabase::class.java, "jangbogi.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        repo = JangbogiRepository(db)
        listId = repo.createList("이번 주 장보기")
        repo.selectList(listId)
        val dao = db.shoppingItemDao()
        dao.insert(ShoppingItem(listId = listId, name = "우유", quantity = 2, category = Category.DAIRY, createdAt = 1))
        dao.insert(ShoppingItem(listId = listId, name = "사과", category = Category.FRUIT, createdAt = 2))
        val id = dao.insert(ShoppingItem(listId = listId, name = "여행에 가져갈 휴대용 무선 이어폰", quantity = 2,
            createdAt = 3, preferredStore = "동네 마트", stockQuantity = 1, purchaseIntent = PurchaseIntent.CONSIDER))
        before = requireNotNull(dao.getById(id))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("고민 중 1").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun close() = runBlocking {
        compose.mainClock.autoAdvance = true
        scenario?.close()
        if (::repo.isInitialized && listId > 0) repo.deleteList(listId)
        if (::db.isInitialized) db.close()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val resolver = instrumentation.targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "v1.3.0-$captureMode-$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JangbogiQa")
        }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    private fun considering() {
        compose.onNodeWithText("고민 중 1").performClick()
        compose.onNodeWithText("결정할래요").performScrollTo().assertIsDisplayed()
    }

    @Test fun readyCancelLeavesDataUntouchedAndDirectBuyCanBeUndone() = runBlocking {
        capture("home")
        if (InstrumentationRegistry.getArguments().getString("captureExtras") == "true") {
            compose.onNodeWithText("이번 주 장보기").performClick()
            compose.onNodeWithText("내 장보기 목록").assertIsDisplayed()
            capture("lists")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText("살림 계획").performClick()
            compose.onNodeWithText("필요한 순간에, 알뜰하게").assertIsDisplayed()
            capture("plan")
            compose.onNodeWithContentDescription("뒤로가기").performClick()
        }
        considering(); capture("consider")
        if (InstrumentationRegistry.getArguments().getString("captureExtras") == "true") {
            compose.onNodeWithText(before.name).performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("항목 편집").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("항목 편집").assertIsDisplayed()
            capture("editor")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            Assert.assertEquals(before, db.shoppingItemDao().getById(before.id))
        }
        compose.onNodeWithText("결정할래요").performClick()
        capture("roulette-ready")
        compose.onNodeWithText("지금은 닫기").performScrollTo().performClick()
        Assert.assertEquals(before, db.shoppingItemDao().getById(before.id))
        compose.onNodeWithContentDescription("${before.name} 살래요").performScrollTo().performClick()
        compose.onNodeWithText("실행 취소").performClick()
        compose.waitForIdle()
        Assert.assertEquals(before, db.shoppingItemDao().getById(before.id))
    }

    @Test fun recreationKeepsDecisionAndConsideringTabAndUndoRestoresItem() = runBlocking {
        considering()
        compose.onNodeWithText("결정할래요").performClick()
        compose.onNodeWithText("돌리기").performScrollTo()
        compose.mainClock.autoAdvance = animationsDisabled
        compose.onNodeWithText("돌리기").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { }
        if (!animationsDisabled) {
            compose.mainClock.advanceTimeBy(100)
            capture("roulette-spinning")
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText("돌리는 중…").performScrollTo().assertIsDisplayed()
            scenario!!.recreate()
            compose.mainClock.advanceTimeBy(100)
            compose.mainClock.autoAdvance = true
        }
        compose.waitUntil(8_000) { compose.onAllNodesWithText("확인").fetchSemanticsNodes().isNotEmpty() }
        val after = db.shoppingItemDao().getById(before.id)
        if (after == null) compose.onNodeWithText("고민 중에서 삭제했어요").performScrollTo().assertIsDisplayed()
        else {
            Assert.assertEquals(before.copy(purchaseIntent = PurchaseIntent.BUY), after)
            compose.onNodeWithText("살 것으로 옮겼어요").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("실행 취소").assertDoesNotExist()
        capture(if (after == null) "roulette-skip" else "roulette-buy")
        scenario!!.recreate()
        compose.onNodeWithText("확인").performScrollTo().assertIsDisplayed()
        Assert.assertEquals(after, db.shoppingItemDao().getById(before.id))
        compose.onNodeWithText("확인").performClick()
        compose.onNodeWithText("고민되는 물건").assertIsDisplayed()
        compose.onNodeWithText("실행 취소").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("고민 중 1").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertEquals(before, db.shoppingItemDao().getById(before.id))
    }
}
