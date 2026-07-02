package com.hanyeong.keyboard.hangul

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * 한글 조합 엔진 자동 검사 (조합 편).
 *
 * 아래 목록의 각 줄이 테스트 1개입니다. (label, 입력 자모, 기대 결과)
 * 입력 자모를 순서대로 눌렀을 때 결과 글자가 기대값과 같아야 통과합니다.
 * 하나라도 다르면 GitHub 빌드가 실패하여 APK가 만들어지지 않습니다.
 */
@RunWith(Parameterized::class)
class HangulCompositionTest(
    private val label: String,
    private val jamos: String,
    private val expected: String,
) {
    @Test
    fun composes() {
        assertEquals("[$label] 입력 '$jamos'", expected, HangulAutomaton.composeText(jamos))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index} {0}")
        fun cases(): List<Array<Any>> = listOf(
            // ── 기본 초성+중성 (자음 + ㅏ) ────────────────────────────
            arrayOf<Any>("기본-가", "ㄱㅏ", "가"),
            arrayOf<Any>("기본-나", "ㄴㅏ", "나"),
            arrayOf<Any>("기본-다", "ㄷㅏ", "다"),
            arrayOf<Any>("기본-라", "ㄹㅏ", "라"),
            arrayOf<Any>("기본-마", "ㅁㅏ", "마"),
            arrayOf<Any>("기본-바", "ㅂㅏ", "바"),
            arrayOf<Any>("기본-사", "ㅅㅏ", "사"),
            arrayOf<Any>("기본-아", "ㅇㅏ", "아"),
            arrayOf<Any>("기본-자", "ㅈㅏ", "자"),
            arrayOf<Any>("기본-차", "ㅊㅏ", "차"),
            arrayOf<Any>("기본-카", "ㅋㅏ", "카"),
            arrayOf<Any>("기본-타", "ㅌㅏ", "타"),
            arrayOf<Any>("기본-파", "ㅍㅏ", "파"),
            arrayOf<Any>("기본-하", "ㅎㅏ", "하"),

            // ── ㅇ + 모든 중성 (겹모음 조합 포함) ──────────────────────
            arrayOf<Any>("모음-아", "ㅇㅏ", "아"),
            arrayOf<Any>("모음-애", "ㅇㅐ", "애"),
            arrayOf<Any>("모음-야", "ㅇㅑ", "야"),
            arrayOf<Any>("모음-얘", "ㅇㅒ", "얘"),
            arrayOf<Any>("모음-어", "ㅇㅓ", "어"),
            arrayOf<Any>("모음-에", "ㅇㅔ", "에"),
            arrayOf<Any>("모음-여", "ㅇㅕ", "여"),
            arrayOf<Any>("모음-예", "ㅇㅖ", "예"),
            arrayOf<Any>("모음-오", "ㅇㅗ", "오"),
            arrayOf<Any>("모음-와", "ㅇㅗㅏ", "와"),
            arrayOf<Any>("모음-왜", "ㅇㅗㅐ", "왜"),
            arrayOf<Any>("모음-외", "ㅇㅗㅣ", "외"),
            arrayOf<Any>("모음-요", "ㅇㅛ", "요"),
            arrayOf<Any>("모음-우", "ㅇㅜ", "우"),
            arrayOf<Any>("모음-워", "ㅇㅜㅓ", "워"),
            arrayOf<Any>("모음-웨", "ㅇㅜㅔ", "웨"),
            arrayOf<Any>("모음-위", "ㅇㅜㅣ", "위"),
            arrayOf<Any>("모음-유", "ㅇㅠ", "유"),
            arrayOf<Any>("모음-으", "ㅇㅡ", "으"),
            arrayOf<Any>("모음-의", "ㅇㅡㅣ", "의"),
            arrayOf<Any>("모음-이", "ㅇㅣ", "이"),

            // ── 홑받침 (여러 받침 종류) ────────────────────────────────
            arrayOf<Any>("받침-각(ㄱ)", "ㄱㅏㄱ", "각"),
            arrayOf<Any>("받침-안(ㄴ)", "ㅇㅏㄴ", "안"),
            arrayOf<Any>("받침-곧(ㄷ)", "ㄱㅗㄷ", "곧"),
            arrayOf<Any>("받침-갈(ㄹ)", "ㄱㅏㄹ", "갈"),
            arrayOf<Any>("받침-감(ㅁ)", "ㄱㅏㅁ", "감"),
            arrayOf<Any>("받침-갑(ㅂ)", "ㄱㅏㅂ", "갑"),
            arrayOf<Any>("받침-갓(ㅅ)", "ㄱㅏㅅ", "갓"),
            arrayOf<Any>("받침-강(ㅇ)", "ㄱㅏㅇ", "강"),
            arrayOf<Any>("받침-낮(ㅈ)", "ㄴㅏㅈ", "낮"),
            arrayOf<Any>("받침-낯(ㅊ)", "ㄴㅏㅊ", "낯"),
            arrayOf<Any>("받침-녘(ㅋ)", "ㄴㅕㅋ", "녘"),
            arrayOf<Any>("받침-밑(ㅌ)", "ㅁㅣㅌ", "밑"),
            arrayOf<Any>("받침-앞(ㅍ)", "ㅇㅏㅍ", "앞"),
            arrayOf<Any>("받침-좋(ㅎ)", "ㅈㅗㅎ", "좋"),
            arrayOf<Any>("받침-밖(ㄲ)", "ㅂㅏㄲ", "밖"),
            arrayOf<Any>("받침-있(ㅆ)", "ㅇㅣㅆ", "있"),

            // ── 겹받침 ────────────────────────────────────────────────
            arrayOf<Any>("겹받침-넋(ㄳ)", "ㄴㅓㄱㅅ", "넋"),
            arrayOf<Any>("겹받침-앉(ㄵ)", "ㅇㅏㄴㅈ", "앉"),
            arrayOf<Any>("겹받침-않(ㄶ)", "ㅇㅏㄴㅎ", "않"),
            arrayOf<Any>("겹받침-닭(ㄺ)", "ㄷㅏㄹㄱ", "닭"),
            arrayOf<Any>("겹받침-삶(ㄻ)", "ㅅㅏㄹㅁ", "삶"),
            arrayOf<Any>("겹받침-밟(ㄼ)", "ㅂㅏㄹㅂ", "밟"),
            arrayOf<Any>("겹받침-핥(ㄾ)", "ㅎㅏㄹㅌ", "핥"),
            arrayOf<Any>("겹받침-읊(ㄿ)", "ㅇㅡㄹㅍ", "읊"),
            arrayOf<Any>("겹받침-옳(ㅀ)", "ㅇㅗㄹㅎ", "옳"),
            arrayOf<Any>("겹받침-값(ㅄ)", "ㄱㅏㅂㅅ", "값"),

            // ── 겹모음 ────────────────────────────────────────────────
            arrayOf<Any>("겹모음-과", "ㄱㅗㅏ", "과"),
            arrayOf<Any>("겹모음-왜", "ㅇㅗㅐ", "왜"),
            arrayOf<Any>("겹모음-외", "ㅇㅗㅣ", "외"),
            arrayOf<Any>("겹모음-워", "ㅇㅜㅓ", "워"),
            arrayOf<Any>("겹모음-웨", "ㅇㅜㅔ", "웨"),
            arrayOf<Any>("겹모음-위", "ㅇㅜㅣ", "위"),
            arrayOf<Any>("겹모음-의", "ㅇㅡㅣ", "의"),
            arrayOf<Any>("겹모음-화", "ㅎㅗㅏ", "화"),
            arrayOf<Any>("겹모음-봐", "ㅂㅗㅏ", "봐"),
            arrayOf<Any>("겹모음-뭐", "ㅁㅜㅓ", "뭐"),

            // ── 도깨비불(연음): 홑받침이 떨어져 다음 글자 초성이 됨 ──────
            arrayOf<Any>("도깨비-각ㅏ→가가", "ㄱㅏㄱㅏ", "가가"),
            arrayOf<Any>("도깨비-먹ㅓ→머거", "ㅁㅓㄱㅓ", "머거"),
            arrayOf<Any>("도깨비-안ㅏ→아나", "ㅇㅏㄴㅏ", "아나"),
            arrayOf<Any>("도깨비-갈ㅏ→가라", "ㄱㅏㄹㅏ", "가라"),
            arrayOf<Any>("도깨비-감ㅏ→가마", "ㄱㅏㅁㅏ", "가마"),
            arrayOf<Any>("도깨비-밥ㅏ→바바", "ㅂㅏㅂㅏ", "바바"),
            arrayOf<Any>("도깨비-강ㅏ→가아", "ㄱㅏㅇㅏ", "가아"),
            arrayOf<Any>("도깨비-좋ㅏ→조하", "ㅈㅗㅎㅏ", "조하"),
            arrayOf<Any>("도깨비-국ㅓ→구거", "ㄱㅜㄱㅓ", "구거"),

            // ── 도깨비불: 겹받침이면 뒷자음만 이동 (핵심 예: 값+ㅏ→갑사) ─
            arrayOf<Any>("도깨비-값ㅏ→갑사", "ㄱㅏㅂㅅㅏ", "갑사"),
            arrayOf<Any>("도깨비-닭ㅏ→달가", "ㄷㅏㄹㄱㅏ", "달가"),
            arrayOf<Any>("도깨비-앉ㅏ→안자", "ㅇㅏㄴㅈㅏ", "안자"),
            arrayOf<Any>("도깨비-삶ㅏ→살마", "ㅅㅏㄹㅁㅏ", "살마"),
            arrayOf<Any>("도깨비-읊ㅓ→을퍼", "ㅇㅡㄹㅍㅓ", "을퍼"),
            arrayOf<Any>("도깨비-밟ㅏ→발바", "ㅂㅏㄹㅂㅏ", "발바"),
            arrayOf<Any>("도깨비-않ㅏ→안하", "ㅇㅏㄴㅎㅏ", "안하"),
            arrayOf<Any>("도깨비-옳ㅏ→올하", "ㅇㅗㄹㅎㅏ", "올하"),
            arrayOf<Any>("도깨비-넋ㅏ→넉사", "ㄴㅓㄱㅅㅏ", "넉사"),

            // ── 쌍자음/쌍모음 (Shift로 입력되는 자모) ──────────────────
            arrayOf<Any>("쌍-까", "ㄲㅏ", "까"),
            arrayOf<Any>("쌍-따", "ㄸㅏ", "따"),
            arrayOf<Any>("쌍-빠", "ㅃㅏ", "빠"),
            arrayOf<Any>("쌍-싸", "ㅆㅏ", "싸"),
            arrayOf<Any>("쌍-짜", "ㅉㅏ", "짜"),
            arrayOf<Any>("쌍-깨", "ㄲㅐ", "깨"),
            arrayOf<Any>("쌍-얘", "ㅇㅒ", "얘"),
            arrayOf<Any>("쌍-예", "ㅇㅖ", "예"),
            arrayOf<Any>("쌍-밖", "ㅂㅏㄲ", "밖"),
            arrayOf<Any>("쌍-있", "ㅇㅣㅆ", "있"),
            arrayOf<Any>("쌍-꿈", "ㄲㅜㅁ", "꿈"),

            // ── 엣지 케이스 (미완성/결합 안 되는 경우) ─────────────────
            arrayOf<Any>("엣지-빈입력", "", ""),
            arrayOf<Any>("엣지-자음하나", "ㄱ", "ㄱ"),
            arrayOf<Any>("엣지-모음하나", "ㅏ", "ㅏ"),
            arrayOf<Any>("엣지-자음둘", "ㄱㄴ", "ㄱㄴ"),
            arrayOf<Any>("엣지-겹모음결합", "ㅗㅏ", "ㅘ"),
            arrayOf<Any>("엣지-모음둘비결합", "ㅏㅓ", "ㅏㅓ"),
            arrayOf<Any>("엣지-자음셋", "ㄱㄴㄷ", "ㄱㄴㄷ"),

            // ── 실제 단어/문장 ────────────────────────────────────────
            arrayOf<Any>("단어-안녕하세요", "ㅇㅏㄴㄴㅕㅇㅎㅏㅅㅔㅇㅛ", "안녕하세요"),
            arrayOf<Any>("단어-사과", "ㅅㅏㄱㅗㅏ", "사과"),
            arrayOf<Any>("단어-학교", "ㅎㅏㄱㄱㅛ", "학교"),
            arrayOf<Any>("단어-한글", "ㅎㅏㄴㄱㅡㄹ", "한글"),
            arrayOf<Any>("단어-키보드", "ㅋㅣㅂㅗㄷㅡ", "키보드"),
            arrayOf<Any>("단어-컴퓨터", "ㅋㅓㅁㅍㅠㅌㅓ", "컴퓨터"),
            arrayOf<Any>("단어-대한민국", "ㄷㅐㅎㅏㄴㅁㅣㄴㄱㅜㄱ", "대한민국"),
            arrayOf<Any>("단어-값어치", "ㄱㅏㅂㅅㅇㅓㅊㅣ", "값어치"),
            arrayOf<Any>("단어-읽었다", "ㅇㅣㄹㄱㅇㅓㅆㄷㅏ", "읽었다"),
            arrayOf<Any>("단어-좋아요", "ㅈㅗㅎㅇㅏㅇㅛ", "좋아요"),
        )
    }
}
