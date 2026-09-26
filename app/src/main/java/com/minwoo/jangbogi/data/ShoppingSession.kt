package com.minwoo.jangbogi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_session",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["id"],
        childColumns = ["activeListId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("activeListId")]
)
data class ShoppingSession(
    @PrimaryKey val id: Int = 1,
    val activeListId: Long? = null
)
