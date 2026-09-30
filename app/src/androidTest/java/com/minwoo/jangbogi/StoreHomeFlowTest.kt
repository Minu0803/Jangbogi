package com.minwoo.jangbogi

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.minwoo.jangbogi.data.*
import com.minwoo.jangbogi.domain.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*

class StoreHomeFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var db: JangbogiDatabase
    private lateinit var repo: JangbogiRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private val ids = mutableListOf<Long>()
    private var firstStore = 0L
    private var secondStore = 0L

    @Before fun seed() = runBlocking {
        db = Room.databaseBuilder(instrumentation.targetContext, JangbogiDatabase::class.java, "jangbogi.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        repo = JangbogiRepository(db)
        firstStore = repo.createList("동네 마트").also(ids::add)
        secondStore = repo.createList("주말에 가는 하나로마트 본점").also(ids::add)
        repo.addItem(firstStore, "우유 2")
        repo.addItem(secondStore, "사과 3")
        repo.selectList(firstStore)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun cleanup() = runBlocking {
        scenario?.close()
        ids.forEach { repo.deleteList(it) }
        db.close()
    }

    private fun home() {
        compose.waitUntil(5_000) { compose.onAllNodesWithText("마트 추가").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("마트 추가").assertIsDisplayed()
    }

    private fun open(id: Long) {
        compose.onNodeWithTag("store-home-list").performScrollToNode(hasTestTag("store-$id"))
        compose.onNodeWithTag("store-$id").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("마트 홈으로").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun save() {
        // Scrolling plus IME resize may move the physical touch target; use its accessible action.
        compose.onNodeWithTag("save-store").performScrollTo().assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureStores") != "true") return
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(350)
        val mode = InstrumentationRegistry.getArguments().getString("captureMode") ?: "light"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "v1.4.0-$mode-$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JangbogiQa")
        }
        val resolver = instrumentation.targetContext.contentResolver
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun launchShowsStoreHomeAndStoresKeepTheirOwnItems() {
        home()
        compose.onNodeWithText("어디서 장을 볼까요?").assertIsDisplayed()
        capture("stores")
        open(firstStore)
        compose.onNodeWithText("우유").assertIsDisplayed()
        compose.onNodeWithText("사과").assertDoesNotExist()
        capture("shopping")
        compose.onNodeWithContentDescription("마트 홈으로").performClick()
        home()
        open(secondStore)
        compose.onNodeWithText("사과").assertIsDisplayed()
        compose.onNodeWithText("우유").assertDoesNotExist()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        home()
        scenario!!.recreate()
        home()
    }

    @Test fun addRequiresNameCancelCreatesNothingAndSaveEntersNewStore() = runBlocking {
        home()
        val before = repo.observeListsWithProgress().first().size
        compose.onNodeWithText("마트 추가").performClick()
        compose.onNodeWithTag("save-store").assertIsNotEnabled()
        compose.onNodeWithTag("store-name-input").performTextInput("   ")
        compose.onNodeWithTag("save-store").assertIsNotEnabled()
        capture("add-store")
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        home()
        Assert.assertEquals(before, repo.observeListsWithProgress().first().size)
        compose.onNodeWithText("마트 추가").performClick()
        compose.onNodeWithTag("store-name-input").performTextReplacement("  새 마트  ")
        scenario!!.recreate()
        compose.onNodeWithTag("store-name-input").assertTextContains("  새 마트  ")
        save()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("마트 홈으로").fetchSemanticsNodes().isNotEmpty() }
        val created = repo.observeListsWithProgress().first().single { it.list.name == "새 마트" }
        ids.add(created.list.id)
        Assert.assertEquals(before + 1, repo.observeListsWithProgress().first().size)
        compose.onNodeWithText("새 마트").assertIsDisplayed()
        compose.onNodeWithContentDescription("마트 홈으로").performClick()
        home()
        compose.onNodeWithTag("store-home-list").performScrollToNode(hasTestTag("store-${created.list.id}"))
        compose.onNodeWithTag("store-${created.list.id}").assertIsDisplayed()
        Unit
    }

    @Test fun renameAndDeleteUndoKeepStoreItems() = runBlocking {
        home()
        compose.onNodeWithTag("store-home-list").performScrollToNode(hasTestTag("store-$firstStore"))
        compose.onNodeWithContentDescription("동네 마트 더보기").performScrollTo().performClick()
        compose.onNodeWithText("마트 이름 변경").performClick()
        compose.onNodeWithTag("store-name-input").performTextReplacement("우리 마트")
        save()
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("우리 마트 더보기").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("우리 마트 더보기").performScrollTo().performClick()
        compose.onNodeWithText("마트 삭제").performClick()
        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("실행 취소").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("store-$firstStore").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertEquals("우리 마트", db.shoppingListDao().getById(firstStore)?.name)
        Assert.assertEquals("우유", db.shoppingItemDao().getAllOnce(firstStore).single().name)
    }

    @Test fun shoppingRecreationAndSwitchingPreserveSeparateInputDrafts() {
        home(); open(firstStore)
        compose.onNodeWithTag("quick-add-input").performTextInput("오이")
        scenario!!.recreate()
        compose.onNodeWithTag("quick-add-input").assertTextContains("오이")
        compose.onNodeWithText("동네 마트").performClick()
        compose.onNodeWithText("다른 마트에서 장보기").assertIsDisplayed()
        capture("switch-store")
        compose.onNodeWithText("주말에 가는 하나로마트 본점").performScrollTo().performClick()
        compose.onNodeWithTag("quick-add-input").assertTextContains("")
        compose.onNodeWithText("사과").assertIsDisplayed()
        compose.onNodeWithContentDescription("마트 홈으로").performClick()
        home(); open(firstStore)
        compose.onNodeWithTag("quick-add-input").assertTextContains("오이")
    }

    @Test fun failedStoreSaveKeepsNameAndRetryCreatesExactlyOneStore() = runBlocking {
        home()
        val before = repo.observeListsWithProgress().first().size
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_store_save BEFORE INSERT ON shopping_session BEGIN SELECT RAISE(ABORT, 'test save failure'); END")
        try {
            compose.onNodeWithText("마트 추가").performClick()
            compose.onNodeWithTag("store-name-input").performTextReplacement("저장 실패 검증 마트")
            save()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("저장하지 못했어요. 다시 시도해 주세요").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("store-name-input").assertTextContains("저장 실패 검증 마트")
            Assert.assertEquals(before, repo.observeListsWithProgress().first().size)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_store_save")
            save()
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("마트 홈으로").fetchSemanticsNodes().isNotEmpty() }
            val saved = repo.observeListsWithProgress().first().single { it.list.name == "저장 실패 검증 마트" }
            ids.add(saved.list.id)
            Assert.assertEquals(before + 1, repo.observeListsWithProgress().first().size)
        } finally { db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS fail_store_save") }
    }

    @Test fun newStoreFromSwitcherReturnsHomeAndOpensAddSheet() {
        home(); open(firstStore)
        compose.onNodeWithText("동네 마트").performClick()
        compose.onNodeWithText("＋ 새로운 마트 추가").performScrollTo().performClick()
        compose.onNodeWithText("어떤 마트에 가시나요?").assertIsDisplayed()
        compose.onNodeWithTag("store-name-input").assertTextContains("")
        compose.onNodeWithText("취소").performScrollTo().performClick()
        home()
    }

    @Test fun recreationDuringSaveClosesSheetAndOpensStoreExactlyOnce() = runBlocking {
        home()
        val before = repo.observeListsWithProgress().first().size
        compose.onNodeWithText("마트 추가").performClick()
        compose.onNodeWithTag("store-name-input").performTextReplacement("저장 중 회전 마트")
        val sqlite = db.openHelper.writableDatabase
        sqlite.beginTransaction()
        try {
            save()
            compose.onNodeWithTag("save-store").assertIsNotEnabled()
            scenario!!.recreate()
            compose.onNodeWithTag("save-store").assertIsNotEnabled()
        } finally { sqlite.endTransaction() }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("마트 홈으로").fetchSemanticsNodes().isNotEmpty() }
        val saved = repo.observeListsWithProgress().first().single { it.list.name == "저장 중 회전 마트" }
        ids.add(saved.list.id)
        Assert.assertEquals(before + 1, repo.observeListsWithProgress().first().size)
        compose.onNodeWithTag("save-store").assertDoesNotExist()
        compose.onNodeWithContentDescription("마트 홈으로").performClick()
        home()
        compose.onNodeWithTag("save-store").assertDoesNotExist()
    }

    @Test fun recreationDuringSelectionUsesCurrentNavigationAndBackReturnsHome() {
        home()
        compose.onNodeWithTag("store-home-list").performScrollToNode(hasTestTag("store-$firstStore"))
        val sqlite = db.openHelper.writableDatabase
        sqlite.beginTransaction()
        try {
            compose.onNodeWithTag("store-$firstStore").performClick()
            scenario!!.recreate()
            home()
        } finally { sqlite.endTransaction() }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("마트 홈으로").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("우유").assertIsDisplayed()
        compose.onNodeWithContentDescription("마트 홈으로").performClick()
        home()
    }

    @Test fun emptyHomeOffersFirstStoreAction() = runBlocking {
        scenario!!.close()
        // Only the disposable test database on the dedicated QA emulator is used.
        db.clearAllTables()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        home()
        compose.onNodeWithText("마트 추가").assertIsDisplayed()
        compose.onNodeWithText("자주 가는 마트를 추가하고\n살 것들을 차근차근 담아보세요.").assertIsDisplayed()
        capture("empty-stores")
    }
}
