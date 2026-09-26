package com.minwoo.jangbogi

import com.minwoo.jangbogi.data.ShoppingList
import com.minwoo.jangbogi.domain.ActiveListSelector
import com.minwoo.jangbogi.domain.ListWithProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveListSelectorTest {
    private fun entry(id: Long, date: Long, total: Int, checked: Int) =
        ListWithProgress(ShoppingList(id, "목록$id", date), total, checked)

    @Test fun `마지막으로 선택한 완료 목록도 유지한다`() {
        val lists = listOf(entry(2, 20, 1, 0), entry(1, 10, 2, 2))
        assertEquals(1L, ActiveListSelector.choose(1, lists))
    }

    @Test fun `삭제된 선택 목록은 최신 미완료 목록으로 대체한다`() {
        val lists = listOf(entry(2, 20, 1, 1), entry(3, 10, 2, 0))
        assertEquals(3L, ActiveListSelector.choose(9, lists))
    }

    @Test fun `날짜가 같으면 높은 id를 선택하고 없으면 null이다`() {
        val lists = listOf(entry(2, 10, 0, 0), entry(3, 10, 0, 0))
        assertEquals(3L, ActiveListSelector.choose(null, lists))
        assertNull(ActiveListSelector.choose(null, emptyList()))
    }
}
