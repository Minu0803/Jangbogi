package com.minwoo.jangbogi

import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ShareTextBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareTextBuilderTest {

    private fun item(
        id: Long,
        name: String,
        category: Category,
        quantity: Int = 1,
        createdAt: Long = 0L,
        isChecked: Boolean = false,
        checkedAt: Long? = null
    ) = ShoppingItem(
        id = id,
        listId = 1L,
        name = name,
        quantity = quantity,
        category = category,
        isChecked = isChecked,
        createdAt = createdAt,
        checkedAt = checkedAt
    )

    @Test
    fun `계약 예시 출력 정확 일치`() {
        val items = listOf(
            item(1, "대파", Category.VEGETABLE, createdAt = 1),
            item(2, "양파", Category.VEGETABLE, quantity = 2, createdAt = 2),
            item(3, "우유", Category.DAIRY, quantity = 2, createdAt = 3),
            item(4, "계란", Category.MEAT_EGG, isChecked = true, checkedAt = 10)
        )
        val expected = listOf(
            "🛒 주말 장보기",
            "",
            "🥬 채소: 대파, 양파 ×2",
            "🥛 유제품: 우유 ×2",
            "",
            "✅ 완료: 계란",
            "남은 3개 · 전체 4개"
        ).joinToString("\n")
        assertEquals(expected, ShareTextBuilder.build("주말 장보기", items))
    }

    @Test
    fun `빈 리스트`() {
        val expected = listOf(
            "🛒 새 목록",
            "",
            "남은 0개 · 전체 0개"
        ).joinToString("\n")
        assertEquals(expected, ShareTextBuilder.build("새 목록", emptyList()))
    }

    @Test
    fun `완료 없음`() {
        val items = listOf(
            item(1, "대파", Category.VEGETABLE, createdAt = 1),
            item(2, "우유", Category.DAIRY, quantity = 3, createdAt = 2)
        )
        val expected = listOf(
            "🛒 장보기",
            "",
            "🥬 채소: 대파",
            "🥛 유제품: 우유 ×3",
            "",
            "남은 2개 · 전체 2개"
        ).joinToString("\n")
        assertEquals(expected, ShareTextBuilder.build("장보기", items))
    }

    @Test
    fun `완료만 있으면 섹션 없이 완료 줄`() {
        val items = listOf(
            item(1, "우유", Category.DAIRY, isChecked = true, checkedAt = 20),
            item(2, "계란", Category.MEAT_EGG, isChecked = true, checkedAt = 10)
        )
        val expected = listOf(
            "🛒 장보기",
            "",
            "✅ 완료: 우유, 계란",
            "남은 0개 · 전체 2개"
        ).joinToString("\n")
        assertEquals(expected, ShareTextBuilder.build("장보기", items))
    }
}
