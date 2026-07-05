package com.hanyeong.keyboard.dict

/**
 * 영어 → 사전 항목 '역색인'을 만듭니다. (영→한 역방향 학습용)
 *
 * 기존 사전은 한글로만 조회할 수 있어(한글 → 항목들), 영어로 찾으려면 방향을
 * 뒤집은 표가 하나 더 필요합니다. 이미 메모리에 올라온 사전에서 파생하므로
 * DB를 다시 읽지 않습니다. 네트워크도 쓰지 않습니다.
 *
 * ── 규칙 ─────────────────────────────────────────────────────────────
 *  - 정규화: 소문자 + 앞뒤 공백 제거. (입력 영어의 대소문자와 무관하게 조회)
 *  - 영어 칸이 여러 뜻이면(`,` `;` `/`로 구분: "glutton; pig") 각 조각을 키로.
 *    "make a living"처럼 구(句)면 그 구 전체가 하나의 키가 됩니다.
 *    (구를 단어로 쪼개면 엉뚱한 연결이 생겨서 쪼개지 않습니다.)
 *  - 알파벳/공백/'/-  로만 이뤄진 키만 담습니다.(숫자·괄호 섞인 값은 건너뜀)
 *  - 다의어(한 영어 → 여러 한글): 목록으로 모으되 **1군(발음기호가 있는
 *    풀 카드) 항목을 앞**으로 정렬합니다.
 */
object ReverseIndex {

    /** 한글→항목 사전에서 영어→항목 역색인을 만듭니다. */
    fun build(forward: Map<String, List<DictEntry>>): Map<String, List<DictEntry>> {
        val rev = LinkedHashMap<String, MutableList<DictEntry>>()
        for (list in forward.values) {
            for (entry in list) {
                for (key in keysOf(entry.english)) {
                    val bucket = rev.getOrPut(key) { mutableListOf() }
                    if (entry !in bucket) bucket.add(entry)
                }
            }
        }
        // 1군(발음기호 있음) 우선: 안정 정렬로 앞에 세웁니다.
        return rev.mapValues { (_, v) ->
            v.sortedBy { if (it.pronunciation.isBlank()) 1 else 0 }
        }
    }

    /** 영어 칸 하나를 역색인 키(들)로 바꿉니다. */
    fun keysOf(english: String): List<String> =
        english.split(',', ';', '/')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.all { c -> c in 'a'..'z' || c == ' ' || c == '\'' || c == '-' } }
            .distinct()
}
