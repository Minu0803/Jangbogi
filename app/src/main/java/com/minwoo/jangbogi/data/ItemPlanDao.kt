package com.minwoo.jangbogi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPlanDao {
    @Query("SELECT * FROM item_plans ORDER BY mustBuyBy IS NULL, mustBuyBy, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ItemPlan>>

    @Query("SELECT * FROM item_plans WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ItemPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(plan: ItemPlan): Long
}
