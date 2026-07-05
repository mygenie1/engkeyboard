package com.hanyeong.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * "카드 높이 ≤ 키보드 높이" 천장 규칙 검증.
 *
 * 실제 뷰 측정은 계측 테스트가 필요하지만, 카드가 커지지 않도록 만드는
 * 핵심 계산(CardBounds.clamp)을 여기서 검증합니다. 뷰(BoundedCardScroll)는
 * 측정할 때 이 함수를 사용해 높이를 키보드 이내로 강제합니다.
 */
class CardBoundsTest {

    @Test
    fun `내용이 키보드보다 크면 키보드 높이로 잘린다`() {
        assertEquals(400, CardBounds.clamp(1000, 400))
        assertEquals(400, CardBounds.clamp(Int.MAX_VALUE, 400))
        assertEquals(400, CardBounds.clamp(401, 400))
    }

    @Test
    fun `내용이 작으면 그대로 둔다`() {
        assertEquals(200, CardBounds.clamp(200, 400))
        assertEquals(400, CardBounds.clamp(400, 400))
        assertEquals(0, CardBounds.clamp(0, 400))
    }

    @Test
    fun `키보드 높이를 아직 모르면 제한하지 않는다`() {
        assertEquals(1000, CardBounds.clamp(1000, 0))
        assertEquals(1000, CardBounds.clamp(1000, -5))
    }

    @Test
    fun `어떤 경우에도 결과는 키보드 높이를 넘지 않는다`() {
        val keyboard = 500
        for (desired in listOf(0, 1, 250, 499, 500, 501, 5000, Int.MAX_VALUE)) {
            val h = CardBounds.clamp(desired, keyboard)
            assertEquals(true, h <= keyboard)
        }
    }
}
