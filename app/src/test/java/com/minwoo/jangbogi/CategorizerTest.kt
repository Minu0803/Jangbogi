package com.minwoo.jangbogi

import com.minwoo.jangbogi.domain.Categorizer
import com.minwoo.jangbogi.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorizerTest {

    private fun assertCategory(name: String, expected: Category) =
        assertEquals("category of \"$name\"", expected, Categorizer.categorize(name))

    @Test
    fun `카테고리별 대표 키워드`() {
        assertCategory("대파", Category.VEGETABLE)
        assertCategory("양파", Category.VEGETABLE)
        assertCategory("사과", Category.FRUIT)
        assertCategory("바나나", Category.FRUIT)
        assertCategory("삼겹살", Category.MEAT_EGG)
        assertCategory("계란", Category.MEAT_EGG)
        assertCategory("고등어", Category.SEAFOOD)
        assertCategory("오징어", Category.SEAFOOD)
        assertCategory("우유", Category.DAIRY)
        assertCategory("치즈", Category.DAIRY)
        assertCategory("만두", Category.FROZEN)
        assertCategory("피자", Category.FROZEN)
        assertCategory("과자", Category.SNACK)
        assertCategory("초콜릿", Category.SNACK)
        assertCategory("콜라", Category.BEVERAGE)
        assertCategory("생수", Category.BEVERAGE)
        assertCategory("간장", Category.SEASONING)
        assertCategory("케첩", Category.SEASONING)
        assertCategory("휴지", Category.HOUSEHOLD)
        assertCategory("세제", Category.HOUSEHOLD)
        assertCategory("사료", Category.ETC)
    }

    @Test
    fun `최장 매칭 - 아이스크림은 크림보다 우선`() {
        assertCategory("아이스크림", Category.FROZEN)
        assertCategory("크림", Category.DAIRY)
    }

    @Test
    fun `최장 매칭 - 새우깡은 새우보다 우선`() {
        assertCategory("새우깡", Category.SNACK)
        assertCategory("새우", Category.SEAFOOD)
    }

    @Test
    fun `미지의 이름은 ETC`() {
        assertCategory("볼펜", Category.ETC)
        assertCategory("노트", Category.ETC)
        assertCategory("", Category.ETC)
    }

    @Test
    fun `부분 문자열 매칭과 공백 제거`() {
        assertCategory("유기농 대파", Category.VEGETABLE)
        assertCategory("서울우유 1L", Category.DAIRY)
    }

    @Test
    fun `라틴 소문자화 매칭`() {
        assertCategory("LA갈비", Category.MEAT_EGG)
    }

    @Test
    fun `키워드 120개 이상`() {
        assertTrue("keyword count = ${Categorizer.keywordCount}", Categorizer.keywordCount >= 120)
    }
}
