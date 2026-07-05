package com.hanyeong.keyboard.dict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 영어 어형 → 원형(lemma) 복원 자동 검사.
 *
 *  1) 복원 성공: 대표 활용형이 정확한 원형으로 복원돼야 한다.
 *  2) 오인식 방지: 애매하거나(die/dye) 사전에 없거나 너무 짧은 말은
 *     엉뚱한 원형을 만들지 않아야 한다(= null).
 *
 * 사전(역색인)은 아래 [LEMMAS]로 흉내 낸다. resolveLemma 는 이 집합에 있는
 * 원형만 후보로 인정한다.
 */
class EnglishLemmaTest {

    /** 테스트용 가짜 역색인(원형 집합). */
    private val LEMMAS = setOf(
        // 불규칙 동사 원형
        "be", "have", "do",
        "go", "eat", "make", "take", "come", "get", "give", "know", "see",
        "say", "find", "think", "buy", "sell", "feel", "keep", "leave", "run",
        // 불규칙 복수의 단수 원형
        "child", "man", "foot", "mouse", "person", "life", "knife",
        // 규칙 동사/명사 원형
        "cat", "box", "try", "study", "stop", "like", "walk", "watch", "city",
        "play", "want", "need",
        // 중의성 확인용 (die/dye 둘 다 존재 → "dying"은 애매)
        "die", "dye"
    )

    private fun resolve(surface: String): String? =
        EnglishLemma.resolveLemma(surface) { it in LEMMAS }

    @Test
    fun `불규칙 복원`() {
        val cases = mapOf(
            "went" to "go", "gone" to "go",
            "ate" to "eat", "eaten" to "eat",
            "made" to "make", "took" to "take", "came" to "come",
            "got" to "get", "gave" to "give", "knew" to "know", "saw" to "see",
            "said" to "say", "found" to "find", "thought" to "think",
            "bought" to "buy", "sold" to "sell", "felt" to "feel",
            "kept" to "keep", "left" to "leave", "ran" to "run",
            // 불규칙 복수
            "children" to "child", "men" to "man", "feet" to "foot",
            "mice" to "mouse", "people" to "person", "lives" to "life",
            "knives" to "knife"
        )
        for ((surface, lemma) in cases) {
            assertEquals("불규칙 '$surface' → 원형", lemma, resolve(surface))
        }
    }

    @Test
    fun `규칙 복원`() {
        val cases = mapOf(
            // 복수/3인칭 -s/-es/-ies
            "cats" to "cat", "boxes" to "box", "tries" to "try",
            "watches" to "watch", "cities" to "city", "needs" to "need",
            // 과거 -ed
            "studied" to "study", "stopped" to "stop", "liked" to "like",
            "walked" to "walk", "played" to "play", "wanted" to "want",
            // 진행 -ing
            "going" to "go", "making" to "make", "running" to "run",
            "playing" to "play"
        )
        for ((surface, lemma) in cases) {
            assertEquals("규칙 '$surface' → 원형", lemma, resolve(surface))
        }
    }

    @Test
    fun `중의성이면 미추천`() {
        // "dying" = die + -ing / dye + -ing → 서로 다른 원형 두 개 → 침묵.
        assertNull("dying 은 애매", resolve("dying"))
    }

    @Test
    fun `사전에 없으면 미추천`() {
        // 원형이 역색인에 없으면 추천하지 않는다.
        for (s in listOf("planets", "xyzzy", "zzzs", "flarped")) {
            assertNull("사전에 없는 '$s' 는 미추천", resolve(s))
        }
    }

    @Test
    fun `너무 짧거나 비알파벳이면 미추천`() {
        // 두 글자 이하(go 등)는 직접 조회로 처리되므로 복원기는 침묵.
        for (s in listOf("go", "is", "an", "hi")) {
            assertNull("짧은 '$s' 는 미추천", resolve(s))
        }
        for (s in listOf("don't", "co2", "hello!", "a b")) {
            assertNull("비알파벳 '$s' 는 미추천", resolve(s))
        }
    }

    @Test
    fun `굴절 안 된 원형 자체는 미추천`() {
        // "cat"은 굴절형이 아니라 원형 그 자체 → 복원기는 관여하지 않는다.
        // (호출부의 '직접 조회'가 담당)
        for (s in listOf("cat", "make", "study")) {
            assertNull("원형 '$s' 자체는 복원기 미관여", resolve(s))
        }
    }
}
