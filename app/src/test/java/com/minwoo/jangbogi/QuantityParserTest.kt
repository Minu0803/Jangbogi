package com.minwoo.jangbogi

import com.minwoo.jangbogi.domain.QuantityParser
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityParserTest {

    private fun assertParsed(raw: String, expectedName: String, expectedQty: Int) {
        val parsed = QuantityParser.parse(raw)
        assertEquals("name of \"$raw\"", expectedName, parsed.name)
        assertEquals("quantity of \"$raw\"", expectedQty, parsed.quantity)
    }

    @Test
    fun `공백 구분 수량`() = assertParsed("우유 2", "우유", 2)

    @Test
    fun `수량 없음`() = assertParsed("우유", "우유", 1)

    @Test
    fun `x 구분자`() = assertParsed("콜라x2", "콜라", 2)

    @Test
    fun `대문자 X 구분자`() = assertParsed("콜라X2", "콜라", 2)

    @Test
    fun `곱셈 기호 구분자`() = assertParsed("물 ×3", "물", 3)

    @Test
    fun `x 주변 공백 허용`() = assertParsed("콜라 x 2", "콜라", 2)

    @Test
    fun `세 자리 숫자는 이름의 일부`() = assertParsed("새우 300", "새우 300", 1)

    @Test
    fun `구분자 없이 붙은 숫자는 이름의 일부`() = assertParsed("라면5", "라면5", 1)

    @Test
    fun `이름이 비게 되면 분리하지 않음`() = assertParsed("2", "2", 1)

    @Test
    fun `경계값 99는 수량`() = assertParsed("계란 99", "계란", 99)

    @Test
    fun `경계값 100은 이름의 일부`() = assertParsed("계란 100", "계란 100", 1)

    @Test
    fun `경계값 0은 이름의 일부`() = assertParsed("계란 0", "계란 0", 1)

    @Test
    fun `트림과 연속 공백 정규화`() = assertParsed("  두부   3 ", "두부", 3)

    @Test
    fun `수량 없는 이름도 정규화`() = assertParsed("  구운   김  ", "구운 김", 1)
}
