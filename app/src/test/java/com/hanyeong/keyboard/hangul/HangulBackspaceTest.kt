package com.hanyeong.keyboard.hangul

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * 한글 조합 엔진 자동 검사 (지우기 편).
 *
 * script 표기법:
 *   - 자모 문자(ㄱ, ㅏ ...) = 그 자모를 누름
 *   - '<' = 백스페이스(지우기)
 *   - '/' = 조합 확정 (스페이스나 다른 글자를 쳐서 현재 글자가 고정되는 상황)
 *
 * 한글 지우기는 '글자 통째로'가 아니라 '자모 하나씩' 지워져야 합니다.
 * (예: 간 → 가 → ㄱ). 겹받침/겹모음은 한 부품씩 줄어듭니다.
 */
@RunWith(Parameterized::class)
class HangulBackspaceTest(
    private val label: String,
    private val script: String,
    private val expected: String,
) {
    @Test
    fun edits() {
        assertEquals("[$label] '$script'", expected, run(script))
    }

    companion object {
        /** script를 순서대로 실행하고, 입력창에 보일 최종 글자를 돌려줍니다. */
        private fun run(script: String): String {
            val a = HangulAutomaton()
            val sb = StringBuilder()
            for (c in script) {
                when (c) {
                    '<' -> when (a.backspace()) {
                        is Back.Composing -> { /* 조합 글자만 바뀜: 아래 composing()에 반영됨 */ }
                        Back.Delete -> if (sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
                    }
                    '/' -> sb.append(a.flush())
                    else -> sb.append(a.press(c).commit)
                }
            }
            return sb.toString() + a.composing()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{index} {0}")
        fun cases(): List<Array<Any>> = listOf(
            arrayOf<Any>("받침 지우기 간→가", "ㄱㅏㄴ<", "가"),
            arrayOf<Any>("받침·모음 간→ㄱ", "ㄱㅏㄴ<<", "ㄱ"),
            arrayOf<Any>("초성까지 간→(빈)", "ㄱㅏㄴ<<<", ""),
            arrayOf<Any>("겹받침 값→갑", "ㄱㅏㅂㅅ<", "갑"),
            arrayOf<Any>("겹받침 값→가", "ㄱㅏㅂㅅ<<", "가"),
            arrayOf<Any>("겹모음 화→호", "ㅎㅗㅏ<", "호"),
            arrayOf<Any>("겹모음 화→ㅎ", "ㅎㅗㅏ<<", "ㅎ"),
            arrayOf<Any>("겹모음 의→으", "ㅇㅡㅣ<", "으"),
            arrayOf<Any>("겹받침 닭→달", "ㄷㅏㄹㄱ<", "달"),
            arrayOf<Any>("자음 하나 ㄱ→(빈)", "ㄱ<", ""),
            arrayOf<Any>("모음 하나 ㅏ→(빈)", "ㅏ<", ""),
            arrayOf<Any>("글자 중간 가→ㄱ", "ㄱㅏ<", "ㄱ"),
            arrayOf<Any>("안녕 지우기 →안녀", "ㅇㅏㄴㄴㅕㅇ<", "안녀"),
            arrayOf<Any>("쌍자음 까→ㄲ", "ㄲㅏ<", "ㄲ"),
            arrayOf<Any>("쌍받침 밖→바", "ㅂㅏㄲ<", "바"),
            arrayOf<Any>("쌍받침 있→이", "ㅇㅣㅆ<", "이"),
            arrayOf<Any>("확정 후 실제삭제 가→(빈)", "ㄱㅏ/<", ""),
            arrayOf<Any>("두 글자 확정 후 삭제 →가", "ㄱㅏ/ㄴㅏ/<", "가"),
            arrayOf<Any>("도깨비 후 지우기 가가→가ㄱ", "ㄱㅏㄱㅏ<", "가ㄱ"),
            arrayOf<Any>("값어치 일부 지우기", "ㄱㅏㅂㅅㅇㅓㅊㅣ<", "값어ㅊ"),
        )
    }
}
