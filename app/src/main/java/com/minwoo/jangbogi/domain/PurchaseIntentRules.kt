package com.minwoo.jangbogi.domain

import com.minwoo.jangbogi.data.ShoppingItem

object PurchaseIntentRules {
    fun move(item: ShoppingItem, intent: PurchaseIntent): ShoppingItem =
        item.copy(purchaseIntent = intent, isChecked = false, checkedAt = null)

    fun toggle(item: ShoppingItem, now: Long): ShoppingItem {
        if (item.purchaseIntent == PurchaseIntent.CONSIDER) return item
        val checked = !item.isChecked
        return item.copy(isChecked = checked, checkedAt = if (checked) now else null)
    }
}
