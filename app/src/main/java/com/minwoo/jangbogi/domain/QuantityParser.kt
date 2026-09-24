package com.minwoo.jangbogi.domain

object QuantityParser {

    data class Parsed(val name: String, val quantity: Int)

    private val whitespace = Regex("""\s+""")

    // 끝부분 (공백* [xX×] 공백* 숫자) 또는 (공백+ 숫자)만 수량으로 인정
    private val qtyTail = Regex("""^(.*?)(?:\s*[xX×]\s*|\s+)(\d+)$""")

    fun parse(raw: String): Parsed {
        val normalized = raw.trim().replace(whitespace, " ")
        val match = qtyTail.matchEntire(normalized) ?: return Parsed(normalized, 1)
        val name = match.groupValues[1].trim()
        val quantity = match.groupValues[2].toIntOrNull()
        return if (name.isEmpty() || quantity == null || quantity !in 1..99) {
            Parsed(normalized, 1)
        } else {
            Parsed(name, quantity)
        }
    }
}
