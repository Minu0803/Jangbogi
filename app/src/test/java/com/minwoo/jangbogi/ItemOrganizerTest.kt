package com.minwoo.jangbogi

import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ItemOrganizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemOrganizerTest {

    private fun item(
        id: Long,
        name: String,
        category: Category,
        createdAt: Long = 0L,
        isChecked: Boolean = false,
        checkedAt: Long? = null
    ) = ShoppingItem(
        id = id,
        listId = 1L,
        name = name,
        category = category,
        isChecked = isChecked,
        createdAt = createdAt,
        checkedAt = checkedAt
    )

    @Test
    fun `섹션은 Category 선언 순서`() {
        val items = listOf(
            item(1, "휴지", Category.HOUSEHOLD),
            item(2, "우유", Category.DAIRY),
            item(3, "대파", Category.VEGETABLE),
            item(4, "사과", Category.FRUIT)
        )
        val sections = ItemOrganizer.sections(items)
        assertEquals(
            listOf(Category.VEGETABLE, Category.FRUIT, Category.DAIRY, Category.HOUSEHOLD),
            sections.map { it.category }
        )
    }

    @Test
    fun `섹션 내부는 createdAt 오름차순`() {
        val items = listOf(
            item(1, "양파", Category.VEGETABLE, createdAt = 30),
            item(2, "대파", Category.VEGETABLE, createdAt = 10),
            item(3, "마늘", Category.VEGETABLE, createdAt = 20)
        )
        val sections = ItemOrganizer.sections(items)
        assertEquals(1, sections.size)
        assertEquals(listOf("대파", "마늘", "양파"), sections[0].items.map { it.name })
    }

    @Test
    fun `체크된 항목은 섹션에서 제외`() {
        val items = listOf(
            item(1, "대파", Category.VEGETABLE),
            item(2, "양파", Category.VEGETABLE, isChecked = true, checkedAt = 5),
            item(3, "우유", Category.DAIRY, isChecked = true, checkedAt = 7)
        )
        val sections = ItemOrganizer.sections(items)
        assertEquals(listOf(Category.VEGETABLE), sections.map { it.category })
        assertEquals(listOf("대파"), sections[0].items.map { it.name })
    }

    @Test
    fun `빈 카테고리는 섹션 없음`() {
        val sections = ItemOrganizer.sections(listOf(item(1, "우유", Category.DAIRY)))
        assertEquals(1, sections.size)
        assertTrue(sections.none { it.items.isEmpty() })
        assertEquals(emptyList<Any>(), ItemOrganizer.sections(emptyList()))
    }

    @Test
    fun `완료 목록은 checkedAt 내림차순 null은 뒤`() {
        val items = listOf(
            item(1, "대파", Category.VEGETABLE),
            item(2, "계란", Category.MEAT_EGG, isChecked = true, checkedAt = 10),
            item(3, "우유", Category.DAIRY, isChecked = true, checkedAt = 30),
            item(4, "김", Category.SEAFOOD, isChecked = true, checkedAt = null),
            item(5, "빵", Category.SNACK, isChecked = true, checkedAt = 20)
        )
        val completed = ItemOrganizer.completed(items)
        assertEquals(listOf("우유", "빵", "계란", "김"), completed.map { it.name })
    }

    @Test
    fun `완료 목록은 미체크 항목 제외`() {
        val items = listOf(
            item(1, "대파", Category.VEGETABLE),
            item(2, "계란", Category.MEAT_EGG, isChecked = true, checkedAt = 10)
        )
        assertEquals(listOf("계란"), ItemOrganizer.completed(items).map { it.name })
    }
}
