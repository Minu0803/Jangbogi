package com.minwoo.jangbogi

import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.ShoppingSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingSummaryTest {
    private fun item(id: Long, intent: PurchaseIntent, checked: Boolean = false) = ShoppingItem(
        id = id, listId = 1, name = "물건$id", createdAt = id,
        purchaseIntent = intent, isChecked = checked,
        checkedAt = if (checked) id else null
    )

    @Test fun `고민 중은 장보기 진행률에서 빠진다`() {
        val items = (1L..4L).map { item(it, PurchaseIntent.BUY) } +
            (5L..6L).map { item(it, PurchaseIntent.BUY, true) } +
            (7L..9L).map { item(it, PurchaseIntent.CONSIDER) }
        val result = ShoppingSummary.from(items)
        assertEquals(4, result.remainingCount)
        assertEquals(2, result.completedCount)
        assertEquals(3, result.consideringCount)
        assertEquals(6, result.purchaseCount)
        assertEquals(2f / 6f, result.progress!!, 0.0001f)
    }

    @Test fun `고민 항목만 있으면 구매 완료로 표시하지 않는다`() {
        val result = ShoppingSummary.from(listOf(item(1, PurchaseIntent.CONSIDER)))
        assertEquals(0, result.purchaseCount)
        assertEquals(1, result.consideringCount)
        assertNull(result.progress)
    }
}
