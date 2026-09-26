package com.minwoo.jangbogi.domain

import com.minwoo.jangbogi.data.ShoppingItem

object ShareTextBuilder {

    fun build(listName: String, items: List<ShoppingItem>): String {
        val purchaseItems = items.filter { it.purchaseIntent == PurchaseIntent.BUY }
        val lines = mutableListOf("🛒 $listName", "")

        val sections = ItemOrganizer.sections(purchaseItems)
        sections.forEach { section ->
            val joined = section.items.joinToString(", ") { item ->
                if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name
            }
            lines += "${section.category.emoji} ${section.category.display}: $joined"
        }
        if (sections.isNotEmpty()) lines += ""

        val completed = ItemOrganizer.completed(purchaseItems)
        if (completed.isNotEmpty()) {
            lines += "✅ 완료: ${completed.joinToString(", ") { it.name }}"
        }

        val remaining = purchaseItems.count { !it.isChecked }
        lines += "남은 ${remaining}개 · 전체 ${purchaseItems.size}개"

        return lines.joinToString("\n")
    }
}
