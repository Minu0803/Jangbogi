package com.minwoo.jangbogi

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.minwoo.jangbogi.data.*
import com.minwoo.jangbogi.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Used against both APK versions to prove an in-place update preserves real on-device data. */
class UpgradeFixtureTest {
    @Test fun preservesItemsIntentsAndPlansAcrossApkUpdate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mode = InstrumentationRegistry.getArguments().getString("upgradeMode") ?: "roundtrip"
        val preferences = context.getSharedPreferences("upgrade-test-fixture", 0)
        val db = Room.databaseBuilder(context, JangbogiDatabase::class.java, "jangbogi.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        val repo = JangbogiRepository(db)
        var listId = preferences.getLong("list", 0)
        try {
            if (mode != "verify") {
                if (mode == "seed") assertEquals(InstrumentationRegistry.getArguments().getString("sourceVersion") ?: "1.2.0", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
                listId = repo.createList("APK 업데이트 검증")
                repo.selectList(listId)
                repo.addItem(listId, "update-milk 3", PurchaseIntent.BUY)
                repo.addItem(listId, "update-eggs 2", PurchaseIntent.BUY)
                repo.addItem(listId, "update-headphones 4", PurchaseIntent.CONSIDER)
                val dao = db.shoppingItemDao()
                repo.toggleItem(requireNotNull(dao.findByName(listId, "update-eggs")).id)
                val considered = requireNotNull(dao.findByName(listId, "update-headphones"))
                assertEquals(UpdateItemResult.UPDATED, repo.updateItemAndPlan(considered.id, considered.name, 4,
                    Category.ETC, 1_800_000_000_000, "동네 마트", 1_801_000_000_000, 10, 2))
                assertTrue(preferences.edit().putLong("list", listId).commit())
            }
            if (mode == "verify") assertEquals("1.4.0", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            assertEquals(listId, repo.resolveActiveListId())
            val dao = db.shoppingItemDao()
            assertEquals(3, dao.getAllOnce(listId).size)
            assertEquals(3, dao.findByName(listId, "update-milk")?.quantity)
            val checked = requireNotNull(dao.findByName(listId, "update-eggs"))
            assertTrue(checked.isChecked); assertNotNull(checked.checkedAt); assertEquals(2, checked.quantity)
            val considered = requireNotNull(dao.findByName(listId, "update-headphones"))
            assertEquals(PurchaseIntent.CONSIDER, considered.purchaseIntent)
            assertFalse(considered.isChecked); assertEquals(4, considered.quantity)
            assertEquals("동네 마트", considered.preferredStore)
            assertEquals(1_800_000_000_000L, considered.plannedBuyAt)
            assertEquals(1_801_000_000_000L, considered.mustBuyBy)
            assertEquals(10, considered.stockUpMonth); assertEquals(2, considered.stockQuantity)
            val plan = requireNotNull(db.itemPlanDao().getByName("update-headphones"))
            assertEquals(4, plan.quantity); assertEquals("동네 마트", plan.preferredStore)
            assertEquals(1_801_000_000_000L, plan.mustBuyBy); assertEquals(2, plan.stockQuantity)
            if (mode == "roundtrip") { repo.deleteList(listId); db.itemPlanDao().deleteByName("update-headphones") }
        } finally { db.close() }
    }
}
