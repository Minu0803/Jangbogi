package com.minwoo.jangbogi.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ShoppingList::class, ShoppingItem::class, ItemHistory::class, ItemPlan::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CategoryConverter::class)
abstract class JangbogiDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun itemHistoryDao(): ItemHistoryDao
    abstract fun itemPlanDao(): ItemPlanDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN plannedBuyAt INTEGER")
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN preferredStore TEXT")
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN mustBuyBy INTEGER")
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN stockUpMonth INTEGER")
        db.execSQL("ALTER TABLE shopping_items ADD COLUMN stockQuantity INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS item_plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL, category TEXT NOT NULL, plannedBuyAt INTEGER, preferredStore TEXT, mustBuyBy INTEGER, stockUpMonth INTEGER, stockQuantity INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_item_plans_name ON item_plans(name)")
    }
}
