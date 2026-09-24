package com.minwoo.jangbogi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.minwoo.jangbogi.domain.Category

@Entity(tableName = "item_plans", indices = [Index(value = ["name"], unique = true)])
data class ItemPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val category: Category = Category.ETC,
    val plannedBuyAt: Long? = null,
    val preferredStore: String? = null,
    val mustBuyBy: Long? = null,
    val stockUpMonth: Int? = null,
    val stockQuantity: Int = 0
)
