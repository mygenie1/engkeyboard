package com.hanyeong.keyboard.dict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 영→한 역색인(ReverseIndex) 스모크 테스트. */
class ReverseIndexTest {

    private fun e(kor: String, eng: String, pron: String = "") =
        DictEntry(kor, eng, pron, "", "")

    @Test
    fun `키 정규화와 분리`() {
        assertEquals(listOf("apple"), ReverseIndex.keysOf("Apple"))
        assertEquals(listOf("glutton", "pig"), ReverseIndex.keysOf("glutton; pig"))
        assertEquals(listOf("rice", "meal"), ReverseIndex.keysOf("rice, meal"))
        // 구(句)는 통째로 하나의 키 (단어로 쪼개지 않음)
        assertEquals(listOf("make a living"), ReverseIndex.keysOf("make a living"))
    }

    @Test
    fun `다의어는 목록으로 모인다`() {
        val forward = linkedMapOf(
            "채소" to listOf(e("채소", "vegetable")),
            "야채" to listOf(e("야채", "vegetable")),
            "사과" to listOf(e("사과", "apple", "/ˈæpəl/"))
        )
        val rev = ReverseIndex.build(forward)
        assertEquals(2, rev["vegetable"]!!.size)
        assertEquals(setOf("채소", "야채"), rev["vegetable"]!!.map { it.korean }.toSet())
        assertEquals("사과", rev["apple"]!!.single().korean)
    }

    @Test
    fun `1군(발음기호 있음)이 앞으로 정렬된다`() {
        // 일부러 2군(발음 없음)을 먼저 넣어도, 1군이 앞에 와야 한다.
        val forward = linkedMapOf(
            "빛" to listOf(e("빛", "light")),                 // 2군 (발음 없음)
            "라이트" to listOf(e("라이트", "light", "/laɪt/")) // 1군 (발음 있음)
        )
        val rev = ReverseIndex.build(forward)
        assertEquals("라이트", rev["light"]!!.first().korean)
    }

    @Test
    fun `구는 통째 키, 단어로는 안 걸린다`() {
        val forward = linkedMapOf("먹구름" to listOf(e("먹구름", "dark clouds")))
        val rev = ReverseIndex.build(forward)
        assertTrue(rev.containsKey("dark clouds"))
        assertNull(rev["dark"])
        assertNull(rev["clouds"])
    }
}
