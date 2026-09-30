package com.minwoo.jangbogi

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.UndoToken
import com.minwoo.jangbogi.domain.*
import com.minwoo.jangbogi.ui.components.DecisionRouletteDialog
import com.minwoo.jangbogi.ui.theme.JangbogiTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DecisionRouletteUiTest {
    @get:Rule val compose = createComposeRule()
    private val item = ShoppingItem(id = 10, listId = 2, name = "휴대용 무선 이어폰", createdAt = 1,
        purchaseIntent = PurchaseIntent.CONSIDER)

    @Test fun readyDismissDoesNotStartOrApply() {
        val state = mutableStateOf<DecisionSession?>(DecisionSession(1, item))
        var started = 0
        var finished = 0
        compose.setContent { JangbogiTheme {
            state.value?.let { DecisionRouletteDialog(it, { started++ }, { finished++ }, { state.value = null }, {}) }
        } }
        compose.onNodeWithText("지금은 닫기").performScrollTo().performClick()
        compose.runOnIdle { assertNull(state.value); assertEquals(0, started); assertEquals(0, finished) }
    }

    @Test fun buyAnimationCompletesOnceAndResultWaitsForConfirmation() = verifyResult(DecisionOutcome.BUY, "살 것으로 옮겼어요")
    @Test fun skipAnimationCompletesOnceAndResultWaitsForConfirmation() = verifyResult(DecisionOutcome.SKIP, "고민 중에서 삭제했어요")

    private fun verifyResult(outcome: DecisionOutcome, message: String) {
        val state = mutableStateOf<DecisionSession?>(DecisionSession(1, item))
        var completed = 0
        var dismissed = 0
        compose.setContent { JangbogiTheme {
            state.value?.let { session -> DecisionRouletteDialog(session,
                onStart = { state.value = session.start(RouletteSpin(outcome, if (outcome == DecisionOutcome.BUY) 2070f else 1890f)) },
                onAnimationFinished = { id ->
                    assertEquals(1L, id)
                    completed++
                    val applying = requireNotNull(state.value?.finishAnimation())
                    state.value = applying.succeed(UndoToken(1, item, if (outcome == DecisionOutcome.BUY) item.copy(purchaseIntent = PurchaseIntent.BUY) else null))
                },
                onDismiss = { dismissed++; state.value = null }, onRetry = {}) }
        } }
        compose.onNodeWithText("돌리기").performScrollTo().performClick()
        compose.waitUntil(8_000) { state.value?.phase == DecisionPhase.RESULT }
        compose.onNodeWithText(message).performScrollTo().assertIsDisplayed()
        compose.mainClock.advanceTimeBy(10_000)
        compose.runOnIdle { assertEquals(1, completed); assertEquals(0, dismissed) }
        compose.onNodeWithText("확인").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, dismissed); assertNull(state.value) }
    }
}
