package com.hanyeong.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View

/**
 * 키보드의 '본체'입니다.
 *
 * 안드로이드는 키보드를 InputMethodService 라는 특별한 부품으로 다룹니다.
 * 입력창을 누르면 안드로이드가 이 부품에게 "키보드 화면 좀 줘" 라고 요청하고,
 * 우리는 onCreateInputView 에서 화면(빈 판)을 만들어 돌려줍니다.
 *
 * 마일스톤 1에서는 글자 없는 회색 빈 판만 띄웁니다.
 * 실제 자판과 한글 조합은 마일스톤 2에서 추가합니다.
 */
class KoreanImeService : InputMethodService() {

    override fun onCreateInputView(): View {
        return layoutInflater.inflate(R.layout.keyboard_view, null)
    }
}
