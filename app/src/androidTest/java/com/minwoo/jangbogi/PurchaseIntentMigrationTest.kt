package com.minwoo.jangbogi

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.minwoo.jangbogi.data.JangbogiDatabase
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.data.MIGRATION_1_2
import com.minwoo.jangbogi.data.MIGRATION_2_3
import com.minwoo.jangbogi.domain.PurchaseIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PurchaseIntentMigrationTest {
    @Test fun versionOneItemsSurviveBothMigrations() = verifyMigration(1)

    @Test fun versionTwoItemsAndPlansSurviveIntentMigration() = verifyMigration(2)

    private fun verifyMigration(oldVersion: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-${UUID.randomUUID()}"
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            createLegacyTables(legacy, oldVersion)
            legacy.version = oldVersion
        } finally {
            legacy.close()
        }

        val upgraded = Room.databaseBuilder(context, JangbogiDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        try {
            runBlocking {
                val item = requireNotNull(upgraded.shoppingItemDao().getById(10))
                assertEquals(7L, item.listId)
                assertEquals("eggs", item.name)
                assertEquals(3, item.quantity)
                assertEquals(true, item.isChecked)
                assertEquals(123L, item.checkedAt)
                assertEquals(PurchaseIntent.BUY, item.purchaseIntent)
                assertEquals(7L, JangbogiRepository(upgraded).resolveActiveListId())
                if (oldVersion == 2) {
                    assertEquals(555L, item.plannedBuyAt)
                    assertEquals("market", item.preferredStore)
                    assertEquals(4, item.stockQuantity)
                    assertNotNull(upgraded.itemPlanDao().getByName("coffee"))
                } else {
                    assertNull(item.plannedBuyAt)
                    assertNull(upgraded.itemPlanDao().getByName("coffee"))
                }
            }
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }

    private fun createLegacyTables(db: android.database.sqlite.SQLiteDatabase, version: Int) {
        db.execSQL("CREATE TABLE shopping_lists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        val planColumns = if (version == 2) ", plannedBuyAt INTEGER, preferredStore TEXT, mustBuyBy INTEGER, stockUpMonth INTEGER, stockQuantity INTEGER NOT NULL DEFAULT 0" else ""
        db.execSQL("CREATE TABLE shopping_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, listId INTEGER NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL, category TEXT NOT NULL, isChecked INTEGER NOT NULL, createdAt INTEGER NOT NULL, checkedAt INTEGER$planColumns, FOREIGN KEY(listId) REFERENCES shopping_lists(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_shopping_items_listId ON shopping_items(listId)")
        db.execSQL("CREATE TABLE item_history (name TEXT NOT NULL, category TEXT NOT NULL, useCount INTEGER NOT NULL, lastUsedAt INTEGER NOT NULL, PRIMARY KEY(name))")
        if (version == 2) {
            db.execSQL("CREATE TABLE item_plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL, category TEXT NOT NULL, plannedBuyAt INTEGER, preferredStore TEXT, mustBuyBy INTEGER, stockUpMonth INTEGER, stockQuantity INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX index_item_plans_name ON item_plans(name)")
            db.execSQL("INSERT INTO item_plans (name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity) VALUES ('coffee', 1, 'BEVERAGE', 999, 'shop', 1000, 12, 2)")
        }
        db.execSQL("INSERT INTO shopping_lists (id, name, createdAt) VALUES (7, 'old list', 100)")
        if (version == 2) {
            db.execSQL("INSERT INTO shopping_items (id, listId, name, quantity, category, isChecked, createdAt, checkedAt, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity) VALUES (10, 7, 'eggs', 3, 'MEAT_EGG', 1, 111, 123, 555, 'market', 666, 9, 4)")
        } else {
            db.execSQL("INSERT INTO shopping_items (id, listId, name, quantity, category, isChecked, createdAt, checkedAt) VALUES (10, 7, 'eggs', 3, 'MEAT_EGG', 1, 111, 123)")
        }
    }
}
