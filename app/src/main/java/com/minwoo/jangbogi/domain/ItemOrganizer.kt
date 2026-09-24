package com.minwoo.jangbogi.domain

import com.minwoo.jangbogi.data.ShoppingItem

object ItemOrganizer {

    fun sections(items: List<ShoppingItem>): List<CategorySection> {
        val unchecked = items.filterNot { it.isChecked }
        return Category.entries.mapNotNull { category ->
            val matched = unchecked
                .filter { it.category == category }
                .sortedWith(compareBy({ it.createdAt }, { it.id }))
            if (matched.isEmpty()) null else CategorySection(category, matched)
        }
    }

    fun completed(items: List<ShoppingItem>): List<ShoppingItem> =
        items.filter { it.isChecked }
            .sortedByDescending { it.checkedAt ?: Long.MIN_VALUE }
}
