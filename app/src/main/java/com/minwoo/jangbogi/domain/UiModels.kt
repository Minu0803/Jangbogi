package com.minwoo.jangbogi.domain

import androidx.room.Embedded
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.ShoppingList

// @Embedded는 DAO JOIN 집계를 이 모델로 직접 매핑하기 위함 (room-common은 순수 JVM 어노테이션)
data class ListWithProgress(
    @Embedded val list: ShoppingList,
    val totalCount: Int,
    val checkedCount: Int
)

data class CategorySection(
    val category: Category,
    val items: List<ShoppingItem>
)

data class Suggestion(
    val name: String,
    val category: Category
)

data class ListUiState(
    val listName: String = "",
    val sections: List<CategorySection> = emptyList(),
    val completedItems: List<ShoppingItem> = emptyList(),
    val totalCount: Int = 0,
    val checkedCount: Int = 0,
    val isLoading: Boolean = true
)
