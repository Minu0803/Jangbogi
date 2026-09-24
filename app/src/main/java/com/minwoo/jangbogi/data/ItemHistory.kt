package com.minwoo.jangbogi.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minwoo.jangbogi.domain.Category

@Entity(tableName = "item_history")
data class ItemHistory(
    @PrimaryKey val name: String,
    val category: Category,
    val useCount: Int = 1,
    val lastUsedAt: Long
)
