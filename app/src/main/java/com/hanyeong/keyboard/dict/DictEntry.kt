package com.hanyeong.keyboard.dict

/**
 * 학습 사전의 한 항목입니다. (사전 표의 한 줄에 해당)
 *
 *  korean        = 한글 단어 (예: 사과)
 *  english       = 영어 단어 (예: apple)
 *  pronunciation = 발음기호 (예: /ˈæpəl/)
 *  meaning       = 한글 뜻 (예: 사과)
 *  example       = 짧은 영어 예문
 *
 * 한 한글 단어에 여러 영어 뜻이 있으면(예: 배 → stomach/pear/ship)
 * 같은 korean 값을 가진 항목이 여러 개 있게 됩니다.
 */
data class DictEntry(
    val korean: String,
    val english: String,
    val pronunciation: String,
    val meaning: String,
    val example: String,
)
