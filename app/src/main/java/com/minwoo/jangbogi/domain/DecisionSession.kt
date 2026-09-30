package com.minwoo.jangbogi.domain

import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.UndoToken

enum class DecisionPhase { READY, SPINNING, APPLYING, RESULT, ERROR }

data class DecisionSession(
    val id: Long,
    val item: ShoppingItem,
    val phase: DecisionPhase = DecisionPhase.READY,
    val spin: RouletteSpin? = null,
    val undoToken: UndoToken? = null,
    val error: String? = null
) {
    fun start(chosen: RouletteSpin): DecisionSession? =
        if (phase == DecisionPhase.READY) copy(phase = DecisionPhase.SPINNING, spin = chosen) else null

    fun finishAnimation(): DecisionSession? =
        if (phase == DecisionPhase.SPINNING && spin != null) copy(phase = DecisionPhase.APPLYING) else null

    fun succeed(token: UndoToken): DecisionSession? =
        if (phase == DecisionPhase.APPLYING) copy(phase = DecisionPhase.RESULT, undoToken = token, error = null) else null

    fun fail(message: String): DecisionSession? =
        if (phase == DecisionPhase.APPLYING) copy(phase = DecisionPhase.ERROR, error = message) else null

    fun retry(): DecisionSession? =
        if (phase == DecisionPhase.ERROR && spin != null) copy(phase = DecisionPhase.APPLYING, error = null) else null
}
