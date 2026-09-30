package com.minwoo.jangbogi

import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.DecisionOutcome
import com.minwoo.jangbogi.domain.DecisionPhase
import com.minwoo.jangbogi.domain.DecisionSession
import com.minwoo.jangbogi.domain.RouletteSpin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecisionSessionTest {
    private val item = ShoppingItem(id = 1, listId = 2, name = "우유", createdAt = 3)
    private val spin = RouletteSpin(DecisionOutcome.BUY, 2070f)

    @Test fun startAndFinishAreSingleUse() {
        val ready = DecisionSession(1, item)
        val spinning = requireNotNull(ready.start(spin))
        assertNull(spinning.start(spin))
        val applying = requireNotNull(spinning.finishAnimation())
        assertEquals(DecisionPhase.APPLYING, applying.phase)
        assertNull(applying.finishAnimation())
    }

    @Test fun retryKeepsOriginalSpinAndDismissedReadyCannotComplete() {
        val ready = DecisionSession(1, item)
        assertNull(ready.finishAnimation())
        val error = requireNotNull(ready.start(spin)?.finishAnimation()?.fail("저장 오류"))
        val retry = requireNotNull(error.retry())
        assertEquals(spin, retry.spin)
        assertEquals(DecisionPhase.APPLYING, retry.phase)
    }
}
