package com.minwoo.jangbogi.domain

import com.minwoo.jangbogi.data.ShoppingItem

data class ShoppingSummary(
    val remainingCount: Int,
    val completedCount: Int,
    val consideringCount: Int
) {
    val purchaseCount: Int get() = remainingCount + completedCount
    val progress: Float? get() = if (purchaseCount == 0) null else completedCount.toFloat() / purchaseCount

    companion object {
        fun from(items: List<ShoppingItem>): ShoppingSummary = ShoppingSummary(
            remainingCount = items.count { it.purchaseIntent == PurchaseIntent.BUY && !it.isChecked },
            completedCount = items.count { it.purchaseIntent == PurchaseIntent.BUY && it.isChecked },
            consideringCount = items.count { it.purchaseIntent == PurchaseIntent.CONSIDER }
        )
    }
}
