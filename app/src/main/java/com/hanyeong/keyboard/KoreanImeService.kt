package com.hanyeong.keyboard

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.hanyeong.keyboard.dict.DictEntry
import com.hanyeong.keyboard.dict.DictionaryDb
import com.hanyeong.keyboard.hangul.Back
import com.hanyeong.keyboard.hangul.HangulAutomaton
import com.hanyeong.keyboard.hangul.Step

/**
 * 키보드의 '본체'입니다.
 *
 * 하는 일:
 *  1) 자판에서 키를 받아 한글 조합 엔진을 거쳐 입력창에 글자를 씁니다.
 *  2) 지금 타이핑 중인 한글 '단어'를 따로 기억해 두었다가,
 *     그 단어가 학습 사전에 정확히 있으면 추천 바에 영어 단어를 띄웁니다.
 *  3) 다시 타이핑을 시작하면 학습 카드를 닫습니다.
 *
 * 중요: 입력창에 이미 있는 사용자 글자는 절대 건드리지 않습니다.
 *       (추천/카드는 우리가 따로 기억하는 정보로만 판단합니다.)
 */
class KoreanImeService : InputMethodService(), KoreanKeyboardView.Listener {

    private val automaton = HangulAutomaton()
    private lateinit var keyboardView: KoreanKeyboardView

    // 학습 사전을 메모리에 올려 두고 즉시 조회합니다. (한글 단어 → 항목들)
    private lateinit var dictionary: Map<String, List<DictEntry>>

    // 지금 타이핑 중인 단어에서 '이미 확정된 부분'. (조합 중 글자는 automaton이 따로 가짐)
    private val committedWord = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        // 앱을 처음 켜면 이때 사전 파일이 만들어지고 기본 단어가 채워집니다.
        dictionary = DictionaryDb(this).loadAll()
    }

    override fun onCreateInputView(): View {
        keyboardView = KoreanKeyboardView(this)
        keyboardView.listener = this
        return keyboardView
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        resetAll()
        currentInputConnection?.finishComposingText()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetAll()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 회전 시 자판을 새 방향 높이로 다시 그립니다.
        setInputView(onCreateInputView())
    }

    // ── 자판에서 오는 신호들 ─────────────────────────────────────────

    override fun onJamo(jamo: Char) {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()                 // 타이핑 재개 → 카드 닫힘
        val step = automaton.press(jamo)
        applyStep(ic, step)
        committedWord.append(step.commit)
        updateSuggestions()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        when (val r = automaton.backspace()) {
            is Back.Composing -> ic.setComposingText(r.text, 1)
            Back.Delete -> {
                ic.finishComposingText()
                ic.deleteSurroundingText(1, 0)
                if (committedWord.isNotEmpty()) {
                    committedWord.deleteCharAt(committedWord.length - 1)
                } else {
                    // 단어 경계(공백 등)를 넘어 지웠으므로 추적을 초기화
                    clearWordTracking()
                }
            }
        }
        updateSuggestions()
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)
        ic.commitText(" ", 1)
        clearWordTracking()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        clearWordTracking()
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)
        ic.commitText(text, 1)
        clearWordTracking()  // 숫자/기호는 단어 경계로 취급
    }

    // ── 추천 갱신 ────────────────────────────────────────────────────

    /**
     * 지금 타이핑 중인 단어(확정 부분 + 조합 중 글자)가 사전에 정확히 있으면
     * 추천 바에 영어 단어(최대 3개)를 띄웁니다.
     */
    private fun updateSuggestions() {
        val word = committedWord.toString() + automaton.composing()
        val entries = if (word.isEmpty()) emptyList()
        else (dictionary[word] ?: emptyList()).take(MAX_SUGGESTIONS)
        keyboardView.showSuggestions(entries)
    }

    private fun clearWordTracking() {
        committedWord.setLength(0)
        keyboardView.showSuggestions(emptyList())
    }

    private fun resetAll() {
        automaton.reset()
        committedWord.setLength(0)
        if (::keyboardView.isInitialized) {
            keyboardView.setShifted(false)
            keyboardView.hideCard()
            keyboardView.showSuggestions(emptyList())
        }
    }

    // ── 엔진 결과를 입력창에 반영 ────────────────────────────────────

    private fun applyStep(ic: InputConnection, step: Step) {
        if (step.commit.isNotEmpty()) {
            ic.setComposingText(step.commit, 1)
            ic.finishComposingText()
        }
        ic.setComposingText(step.composing, 1)
    }

    private fun finalizeComposing(ic: InputConnection) {
        automaton.flush()
        ic.finishComposingText()
    }

    companion object {
        private const val MAX_SUGGESTIONS = 3
    }
}
