package com.minwoo.jangbogi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShoppingSessionDao {
    @Query("SELECT activeListId FROM shopping_session WHERE id = 1")
    suspend fun getActiveListId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(session: ShoppingSession)
}
