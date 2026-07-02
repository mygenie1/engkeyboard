package com.hanyeong.keyboard.hangul

/**
 * 한글 조합 엔진(오토마타).
 *
 * 이 파일에는 안드로이드 관련 코드가 하나도 없습니다.
 * 순수 코틀린이라서, 마일스톤 3의 자동 테스트에서 이 엔진만 따로 떼어
 * 수백 개의 입력 케이스를 검증할 수 있습니다.
 *
 * 하는 일: 사용자가 누른 자모(ㄱ, ㅏ, ㄴ ...)를 하나씩 받아서
 * 완성된 한글 음절(가, 각, 값 ...)로 조합합니다.
 *
 * 핵심 개념 3가지
 *  - 초성(첫 자음) + 중성(모음) + 종성(받침) = 한 글자
 *  - 겹받침(ㄱ+ㅅ=ㄳ)과 겹모음(ㅗ+ㅏ=ㅘ) 조합
 *  - 도깨비불(연음): 받침이 있는 글자 뒤에 모음이 오면 받침이 떨어져 나가
 *    다음 글자의 첫소리가 됩니다. 예) "값" + ㅏ → "갑사"
 */

/** 한 번 입력했을 때의 결과.
 *  commit    = 이제 확정(고정)해야 하는 글자들 (없으면 빈 문자열)
 *  composing = 현재 조합 중이라 아직 바뀔 수 있는 글자 (없으면 빈 문자열)
 */
data class Step(val commit: String, val composing: String)

/** 지우기(백스페이스) 결과.
 *  Composing = 조합 중인 글자가 이렇게 바뀌었다
 *  Delete    = 조합 중인 글자가 없으니, 입력창의 실제 글자 하나를 지워라
 */
sealed class Back {
    data class Composing(val text: String) : Back()
    object Delete : Back()
}

class HangulAutomaton {

    // 현재 조합 중인 글자의 부품들. -1 = 아직 없음.
    private var cho = -1   // 초성 자리 (초성표의 몇 번째인가)
    private var jung = -1  // 중성 자리 (중성표의 몇 번째인가)
    private var jong = -1  // 종성 자리 (종성표의 몇 번째인가, 1 이상일 때만 받침 있음)

    /** 자모 하나를 입력합니다. */
    fun press(j: Char): Step =
        if (Tables.jungOf.containsKey(j)) pressVowel(j) else pressConsonant(j)

    // ── 자음이 들어왔을 때 ────────────────────────────────────────────
    private fun pressConsonant(c: Char): Step {
        val ci = Tables.choOf[c] ?: return Step("", composing()) // 자음이 아니면 무시
        val asJong = Tables.jongOf[c]                            // 받침이 될 수 있으면 그 번호, 아니면 null

        // 1) 아무것도 없는 상태 → 첫 자음(초성)으로
        if (cho == -1 && jung == -1) {
            cho = ci
            return Step("", composing())
        }
        // 2) 초성만 있고 모음이 없음 → 자음이 연달아 옴: 앞 자음 확정하고 새로 시작
        if (cho >= 0 && jung == -1) {
            val prev = Tables.CHO[cho].toString()
            clear(); cho = ci
            return Step(prev, composing())
        }
        // 3) 초성+중성 있고 받침 없음 → 받침으로 붙이기 시도
        if (cho >= 0 && jung >= 0 && jong <= 0) {
            return if (asJong != null) {
                jong = asJong
                Step("", composing())
            } else {
                // ㄸㅃㅉ처럼 받침이 될 수 없는 자음: 현재 글자 확정하고 새 글자 시작
                val prev = composing(); clear(); cho = ci
                Step(prev, composing())
            }
        }
        // 4) 초성+중성+받침 있음 → 겹받침 시도, 안 되면 새 글자
        if (cho >= 0 && jung >= 0 && jong > 0) {
            val combined = Tables.jongCombine[jong to c]
            return if (combined != null) {
                jong = combined
                Step("", composing())
            } else {
                val prev = composing(); clear(); cho = ci
                Step(prev, composing())
            }
        }
        // 5) 홀로 있던 모음 뒤에 자음: 모음 확정하고 새 글자 시작
        val prev = composing(); clear(); cho = ci
        return Step(prev, composing())
    }

    // ── 모음이 들어왔을 때 ────────────────────────────────────────────
    private fun pressVowel(v: Char): Step {
        val vi = Tables.jungOf[v] ?: return Step("", composing())

        // 1) 아무것도 없음 → 홀로 있는 모음
        if (cho == -1 && jung == -1) {
            jung = vi
            return Step("", composing())
        }
        // 2) 초성만 있음 → 초성+중성 결합
        if (cho >= 0 && jung == -1) {
            jung = vi
            return Step("", composing())
        }
        // 3) 초성+중성, 받침 없음 → 겹모음 시도, 안 되면 새 글자
        if (cho >= 0 && jung >= 0 && jong <= 0) {
            val comb = Tables.jungCombine[jung to v]
            if (comb != null) { jung = comb; return Step("", composing()) }
            val prev = composing(); clear(); jung = vi
            return Step(prev, composing())
        }
        // 4) 홀로 있는 모음 뒤에 모음 → 겹모음 시도, 안 되면 새 모음
        if (cho == -1 && jung >= 0) {
            val comb = Tables.jungCombine[jung to v]
            if (comb != null) { jung = comb; return Step("", composing()) }
            val prev = composing(); clear(); jung = vi
            return Step(prev, composing())
        }
        // 5) 초성+중성+받침 뒤에 모음 → 도깨비불(연음)!
        //    받침이 떨어져 나가 다음 글자의 초성이 됩니다.
        //    겹받침이면 뒷부분만, 홑받침이면 통째로 이동합니다.
        val split = Tables.jongDecompose[jong]
        val remain: Int
        val detached: Char
        if (split != null) {           // 겹받침: 앞부분은 남고 뒷부분이 이동 (예: ㅄ → ㅂ 남고 ㅅ 이동)
            remain = split.first
            detached = split.second
        } else {                        // 홑받침: 통째로 이동
            remain = 0
            detached = Tables.JONG[jong][0]
        }
        val committed = syllable(cho, jung, remain)
        clear()
        cho = Tables.choOf[detached]!!
        jung = vi
        return Step(committed, composing())
    }

    /** 지우기(백스페이스). */
    fun backspace(): Back {
        // 받침이 있으면 받침부터 지움 (겹받침이면 한 부품만 줄임)
        if (cho >= 0 && jung >= 0 && jong > 0) {
            val split = Tables.jongDecompose[jong]
            jong = split?.first ?: -1
            if (jong == 0) jong = -1
            return Back.Composing(composing())
        }
        // 모음이 있으면 모음을 지움 (겹모음이면 한 부품만 줄임)
        if (jung >= 0) {
            val base = Tables.jungDecompose[jung]
            jung = base ?: -1
            return Back.Composing(composing())
        }
        // 초성만 남았으면 초성을 지움
        if (cho >= 0) {
            cho = -1
            return Back.Composing(composing())
        }
        // 조합 중인 게 없음 → 입력창의 실제 글자를 지워야 함
        return Back.Delete
    }

    /** 조합 중인 글자를 확정하고 상태를 비웁니다. (스페이스/엔터 등을 칠 때) */
    fun flush(): String {
        val s = composing()
        clear()
        return s
    }

    /** 조합 상태를 완전히 초기화합니다. (다른 입력창으로 이동할 때) */
    fun reset() = clear()

    /** 현재 조합 중인 글자(아직 확정 안 된 글자)를 문자열로 돌려줍니다. */
    fun composing(): String {
        if (cho == -1 && jung == -1) return ""
        if (cho >= 0 && jung == -1) return Tables.CHO[cho].toString()      // 홑 자음 (ㄱ)
        if (cho == -1 && jung >= 0) return Tables.JUNG[jung].toString()    // 홑 모음 (ㅏ)
        return syllable(cho, jung, if (jong < 0) 0 else jong)              // 완성 글자 (각)
    }

    private fun clear() { cho = -1; jung = -1; jong = -1 }

    /** 초성/중성/종성 번호로 완성된 한글 음절 문자를 만듭니다. */
    private fun syllable(c: Int, j: Int, jo: Int): String =
        (0xAC00 + (c * 21 + j) * 28 + jo).toChar().toString()

    companion object {
        /**
         * 테스트용 도우미: 자모 문자열을 통째로 넣으면 최종 결과 글자를 돌려줍니다.
         * 예) composeText("ㄱㅏㅂㅅㅏ") → "갑사"
         * 마일스톤 3의 단위 테스트가 이 함수를 주로 사용합니다.
         */
        fun composeText(jamos: String): String {
            val a = HangulAutomaton()
            val sb = StringBuilder()
            for (ch in jamos) sb.append(a.press(ch).commit)
            sb.append(a.composing())
            return sb.toString()
        }
    }
}
