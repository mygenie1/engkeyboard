package com.hanyeong.keyboard

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.hanyeong.keyboard.dict.Conjugation
import com.hanyeong.keyboard.dict.DictEntry
import com.hanyeong.keyboard.dict.DictionaryDb
import com.hanyeong.keyboard.dict.ReverseIndex
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

    // 영→한 역방향 학습용 색인 (영어 단어 → 항목들). 사전에서 파생.
    private lateinit var reverseIndex: Map<String, List<DictEntry>>

    // 지금 타이핑 중인 단어에서 '이미 확정된 부분'. (조합 중 글자는 automaton이 따로 가짐)
    private val committedWord = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        // 앱을 처음 켜면 이때 사전 파일이 만들어지고 기본 단어가 채워집니다.
        dictionary = DictionaryDb(this).loadAll()
        // 같은 사전을 뒤집어 영어로도 찾을 수 있게 역색인을 만들어 둡니다.
        reverseIndex = ReverseIndex.build(dictionary)
    }

    // 마지막으로 자판을 그릴 때 쓴 높이 설정. 설정이 바뀌면 다시 그립니다.
    private var appliedHeight: String? = null

    override fun onCreateInputView(): View {
        keyboardView = KoreanKeyboardView(this)
        keyboardView.listener = this
        appliedHeight = Settings.height(this)
        return keyboardView
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        resetAll()
        currentInputConnection?.finishComposingText()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // 설정 화면에서 '키보드 높이'를 바꿨다면 새 높이로 자판을 다시 그립니다.
        if (::keyboardView.isInitialized && appliedHeight != Settings.height(this)) {
            setInputView(onCreateInputView())
        }
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
        refreshSuggestion()
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
                }
                // committedWord가 이미 비어 있으면 단어 경계를 넘어 지운 것 →
                // 아래 검사에서 추천을 없앱니다.
            }
        }
        // 지금 추적 중인 단어가 완전히 비면(= 백스페이스로 단어 자체를 지움)
        // 추천도 함께 없앱니다. 글자가 남아 있으면 추천을 다시 계산합니다.
        if (committedWord.isEmpty() && automaton.composing().isEmpty()) {
            clearSuggestions()
        } else {
            refreshSuggestion()
        }
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)
        ic.commitText(" ", 1)
        resetWordTracking()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        resetWordTracking()
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)
        ic.commitText(text, 1)
        resetWordTracking()  // 영문/숫자/기호/문장부호는 단어 경계로 취급
    }

    /**
     * 한/영·기호판 전환 직전에 호출됩니다.
     * 조합 중이던 한글을 '확정'해 입력창에서 사라지지 않게 하고,
     * 학습용 단어 추적도 초기화합니다.
     */
    override fun onModeSwitch() {
        val ic = currentInputConnection ?: return
        keyboardView.hideCard()
        finalizeComposing(ic)   // automaton.flush() + finishComposingText()
        resetWordTracking()
    }

    // ── 추천 갱신 ────────────────────────────────────────────────────

    /**
     * 추천 '유지' 규칙:
     *  - 지금 타이핑 중인 단어(확정 부분 + 조합 중 글자)가 사전에 있으면
     *    그 단어의 추천(최대 3개)으로 추천 바를 '교체'합니다.
     *  - 사전에 없으면 아무것도 하지 않아, 직전에 띄운 추천을 '그대로 유지'합니다.
     *    → 띄어쓰기·조사·문장부호를 쳐도 추천이 사라지지 않고,
     *      다음 사전 단어가 새로 인식될 때 비로소 교체됩니다.
     * (단어 자체를 백스페이스로 다 지우는 경우만 onBackspace에서 추천을 없앱니다.)
     */
    private fun refreshSuggestion() {
        // 설정에서 추천 바를 끄면 항상 빈 상태로 둡니다. (일반 키보드처럼 동작)
        if (!Settings.suggestionsEnabled(this)) {
            keyboardView.showSuggestions(emptyList())
            return
        }
        val word = committedWord.toString() + automaton.composing()
        if (word.isEmpty()) return

        // 1) 단어가 사전에 그대로 있으면 그 추천으로 교체.
        dictionary[word]?.let {
            keyboardView.showSuggestions(it.take(MAX_SUGGESTIONS))
            return
        }
        // 2) 없으면 용언 활용형인지 살펴 원형(…다)을 복원해 봅니다.
        //    (먹었어 → 먹다) 확신할 때만 그 원형의 추천으로 교체합니다.
        val base = Conjugation.resolveBase(word) { dictionary.containsKey(it) }
        if (base != null) {
            dictionary[base]?.let {
                keyboardView.showSuggestions(it.take(MAX_SUGGESTIONS))
                return
            }
        }
        // 3) 사전에도 없고 원형 복원도 애매하면 직전 추천을 그대로 유지합니다.
    }

    /** 단어 경계를 만났을 때: 추적 중인 단어만 초기화하고 추천은 그대로 둡니다. */
    private fun resetWordTracking() {
        committedWord.setLength(0)
    }

    /** 추천 바를 비웁니다. (단어를 백스페이스로 지웠거나 입력창이 바뀔 때) */
    private fun clearSuggestions() {
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
