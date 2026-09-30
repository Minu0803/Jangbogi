package com.minwoo.jangbogi.domain

import kotlin.random.Random

enum class DecisionOutcome { BUY, SKIP }

data class RouletteSpin(val outcome: DecisionOutcome, val targetRotation: Float)

object DecisionRoulette {
    fun spin(random: Random = Random.Default): RouletteSpin {
        val outcome = if (random.nextBoolean()) DecisionOutcome.BUY else DecisionOutcome.SKIP
        val jitter = random.nextInt(-60, 61)
        val center = if (outcome == DecisionOutcome.BUY) 270 else 90
        return RouletteSpin(outcome, (5 * 360 + center + jitter).toFloat())
    }

    fun outcomeAtRotation(rotation: Float): DecisionOutcome {
        val localAngle = ((-90f - rotation) % 360f + 360f) % 360f
        return if (localAngle > 270f || localAngle < 90f) DecisionOutcome.BUY else DecisionOutcome.SKIP
    }
}
