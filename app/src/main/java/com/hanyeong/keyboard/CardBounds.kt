package com.hanyeong.keyboard

/**
 * 학습 카드 높이 제한 계산(안전장치).
 *
 * 카드는 '키보드 영역 안의 오버레이'라서 **키보드 높이를 절대 넘으면 안 됩니다**
 * (넘으면 입력창·앱 화면을 가림). 이 규칙을 한 줄 함수로 뽑아 단위 테스트로
 * 검증합니다. (실제 뷰 측정은 계측 테스트가 필요해, 여기서는 '천장 계산'을
 * 순수 로직으로 확인하고 뷰에서는 이 함수를 써서 높이를 제한합니다.)
 */
object CardBounds {

    /**
     * 카드 내용이 원하는 높이([desiredPx])를 키보드 높이([keyboardPx]) 이내로
     * 잘라 돌려줍니다.
     *  - 키보드 높이를 아직 모르면(0 이하) 제한하지 않습니다(레이아웃 전 단계).
     *  - 그 외에는 둘 중 작은 값 → 카드 높이 ≤ 키보드 높이 보장.
     */
    fun clamp(desiredPx: Int, keyboardPx: Int): Int =
        if (keyboardPx <= 0) desiredPx else minOf(desiredPx, keyboardPx)
}
