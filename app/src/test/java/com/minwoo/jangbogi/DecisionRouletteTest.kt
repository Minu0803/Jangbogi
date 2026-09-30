package com.minwoo.jangbogi

import com.minwoo.jangbogi.domain.DecisionOutcome
import com.minwoo.jangbogi.domain.DecisionRoulette
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionRouletteTest {
    private class FixedRandom(private val buy: Boolean, private val jitter: Int) : Random() {
        var calls = 0
        override fun nextBits(bitCount: Int): Int {
            calls++
            return if (calls == 1) (if (buy) 1 else 0) else jitter
        }
    }

    @Test fun spinChoosesOnceAndGeometryMatchesBothOutcomes() {
        for (buy in listOf(true, false)) {
            val random = FixedRandom(buy, 0)
            val spin = DecisionRoulette.spin(random)
            assertEquals(if (buy) DecisionOutcome.BUY else DecisionOutcome.SKIP, spin.outcome)
            assertEquals(spin.outcome, DecisionRoulette.outcomeAtRotation(spin.targetRotation))
        }
    }

    @Test fun sectorCentersMatchFixedTopPointer() {
        assertEquals(DecisionOutcome.BUY, DecisionRoulette.outcomeAtRotation(5 * 360f + 270f))
        assertEquals(DecisionOutcome.SKIP, DecisionRoulette.outcomeAtRotation(5 * 360f + 90f))
    }
}
