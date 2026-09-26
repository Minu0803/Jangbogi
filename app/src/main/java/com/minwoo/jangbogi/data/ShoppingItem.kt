package com.minwoo.jangbogi.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.PurchaseIntent

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Int = 1,
    val category: Category = Category.ETC,
    val isChecked: Boolean = false,
    val createdAt: Long,
    val checkedAt: Long? = null,
    val plannedBuyAt: Long? = null,
    val preferredStore: String? = null,
    val mustBuyBy: Long? = null,
    val stockUpMonth: Int? = null,
    @ColumnInfo(defaultValue = "0") val stockQuantity: Int = 0,
    @ColumnInfo(defaultValue = "'BUY'") val purchaseIntent: PurchaseIntent = PurchaseIntent.BUY
)
