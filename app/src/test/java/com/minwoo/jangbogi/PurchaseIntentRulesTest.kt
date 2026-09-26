package com.minwoo.jangbogi

import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.PurchaseIntentRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PurchaseIntentRulesTest {
    @Test fun `완료된 물건을 고민 중으로 옮기면 구매 기록을 풀고 계획은 보존한다`() {
        val item = ShoppingItem(
            id = 4, listId = 2, name = "우유", quantity = 3, createdAt = 10,
            isChecked = true, checkedAt = 20, preferredStore = "동네 마트",
            stockQuantity = 2
        )
        val moved = PurchaseIntentRules.move(item, PurchaseIntent.CONSIDER)
        assertEquals(item.id, moved.id)
        assertEquals(item.quantity, moved.quantity)
        assertEquals(item.preferredStore, moved.preferredStore)
        assertEquals(item.stockQuantity, moved.stockQuantity)
        assertEquals(PurchaseIntent.CONSIDER, moved.purchaseIntent)
        assertFalse(moved.isChecked)
        assertNull(moved.checkedAt)
    }

    @Test fun `고민 중인 물건은 구매 체크가 되지 않는다`() {
        val item = ShoppingItem(id = 1, listId = 1, name = "두부", createdAt = 1, purchaseIntent = PurchaseIntent.CONSIDER)
        assertEquals(item, PurchaseIntentRules.toggle(item, 9))
    }
}
