package com.hanyeong.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.hanyeong.keyboard.hangul.Back
import com.hanyeong.keyboard.hangul.HangulAutomaton
import com.hanyeong.keyboard.hangul.Step

/**
 * 키보드의 '본체'입니다.
 *
 * 자판(KoreanKeyboardView)에서 키를 누르면 여기로 신호가 오고,
 * 한글 조합 엔진(HangulAutomaton)을 거쳐, 입력창에 글자를 써 줍니다.
 *
 * 안드로이드는 조합 중인 글자(아직 바뀔 수 있는 글자)를 'composing text'라는
 * 임시 영역으로 다룹니다. 글자가 확정되면 그 영역을 고정(finish)합니다.
 */
class KoreanImeService : InputMethodService(), KoreanKeyboardView.Listener {

    private val automaton = HangulAutomaton()
    private lateinit var keyboardView: KoreanKeyboardView

    override fun onCreateInputView(): View {
        keyboardView = KoreanKeyboardView(this)
        keyboardView.listener = this
        return keyboardView
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // 새 입력창으로 옮기면 조합 상태를 깨끗이 비웁니다.
        automaton.reset()
        currentInputConnection?.finishComposingText()
        if (::keyboardView.isInitialized) keyboardView.setShifted(false)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        automaton.reset()
    }

    // ── 자판에서 오는 신호들 ─────────────────────────────────────────

    override fun onJamo(jamo: Char) {
        val ic = currentInputConnection ?: return
        applyStep(ic, automaton.press(jamo))
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        when (val r = automaton.backspace()) {
            is Back.Composing -> ic.setComposingText(r.text, 1)
            Back.Delete -> {
                // 조합 중인 글자가 없으면 입력창의 실제 글자 하나를 지웁니다.
                ic.finishComposingText()
                ic.deleteSurroundingText(1, 0)
            }
        }
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        finalizeComposing(ic)
        ic.commitText(" ", 1)
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        finalizeComposing(ic)
        // 앱에 따라 줄바꿈 또는 전송으로 동작합니다.
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        finalizeComposing(ic)
        ic.commitText(text, 1)
    }

    // ── 엔진 결과를 입력창에 반영 ────────────────────────────────────

    /**
     * 조합 결과(Step)를 입력창에 적용합니다.
     * commit 이 있으면 그 부분을 먼저 확정하고, 이어서 새 조합 글자를 임시로 보여 줍니다.
     * 이때 '이미 입력창에 있던 사용자 글자'는 절대 건드리지 않습니다.
     */
    private fun applyStep(ic: InputConnection, step: Step) {
        if (step.commit.isNotEmpty()) {
            ic.setComposingText(step.commit, 1) // 조합 영역을 확정할 글자로 바꾸고
            ic.finishComposingText()            // 고정한 뒤
        }
        ic.setComposingText(step.composing, 1)  // 새 조합 글자를 임시로 표시
    }

    /** 현재 조합 중인 글자를 그대로 확정합니다. */
    private fun finalizeComposing(ic: InputConnection) {
        automaton.flush()
        ic.finishComposingText()
    }
}
