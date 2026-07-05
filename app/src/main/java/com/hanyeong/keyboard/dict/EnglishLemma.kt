package com.hanyeong.keyboard.dict

/**
 * 영어 어형(활용형)에서 사전 원형(lemma)을 복원합니다.
 *
 * 예) "went" → "go",  "boxes" → "box",  "making" → "make",  "children" → "child"
 *
 * 안드로이드 코드가 없는 순수 코틀린이라, 단위 테스트에서 "엉뚱한 원형으로
 * 복원되지 않는지"를 통째로 검증할 수 있습니다.
 *
 * ── 안전 제1원칙 (한국어 활용형 복원기와 동일) ───────────────────────
 *   "틀린 추천이 빈 추천보다 나쁘다." 확신할 수 있을 때만 원형 하나를 돌려주고,
 *   애매하면 null을 돌려줍니다.
 *
 * ── 어떻게 확신하는가 (왕복 검증) ─────────────────────────────────────
 *   활용형에서 원형 후보를 넉넉히 만든 뒤, 그 원형을 '다시 굴절'시켜 입력과
 *   글자 하나까지 똑같아질 때만 인정합니다. 게다가 후보 원형은 반드시
 *   '사전(역색인)에 실제로 있는' 단어여야 합니다. 서로 다른 원형이 2개 이상
 *   나오면(예: "dying" = die/dye) 미추천합니다.
 *
 * ── 규칙 vs 불규칙 ───────────────────────────────────────────────────
 *   - 규칙 굴절(복수 -s/-es, 과거 -ed, 진행 -ing)은 철자 규칙을 코드로 구현.
 *   - 불규칙(went, ate, children …)은 초고빈도만 골라 아래 [IRREGULAR] 표에
 *     활용형→원형으로 손으로 등록.
 *
 * (직접 조회 — "apple"처럼 원형이 그대로 사전에 있는 경우 — 는 호출부에서
 *  먼저 처리합니다. 이 함수는 '굴절된 형태'의 원형 복원만 담당합니다.)
 */
object EnglishLemma {

    /**
     * 영어 활용형 [surface]의 사전 원형을 찾습니다.
     *
     * @param inDict  후보 원형이 사전(역색인)에 있는지 확인하는 함수
     * @return 확신할 수 있는 원형 하나. 후보가 없거나 2개 이상이면 null.
     */
    fun resolveLemma(surface: String, inDict: (String) -> Boolean): String? {
        val s = surface.lowercase()
        // 아주 짧은 말은 굴절보다 그 자체일 확률이 커 애매합니다 → 시도 안 함.
        if (s.length < 3) return null
        // 순수 알파벳만(하이픈·숫자·공백 섞이면 제외).
        if (s.any { it !in 'a'..'z' }) return null

        val candidates = LinkedHashSet<String>()

        // 1) 불규칙 표에서 바로 찾기.
        IRREGULAR[s]?.let { if (inDict(it)) candidates.add(it) }
        // 2) 규칙 어미를 벗겨 원형 후보를 만들고, '다시 굴절'시켜 검증.
        for (lemma in regularCandidates(s)) {
            if (lemma == s) continue          // 굴절 안 된 형태는 여기서 다루지 않음
            if (!inDict(lemma)) continue
            if (s in inflect(lemma)) candidates.add(lemma)
        }

        return if (candidates.size == 1) candidates.first() else null
    }

    // ── 역방향: 활용형 → 원형 후보(과다 생성; 정방향 굴절로 걸러짐) ─────

    /** 활용형에서 규칙 어미를 벗겨 원형 후보들을 넉넉히 만듭니다. */
    private fun regularCandidates(s: String): Set<String> {
        val out = LinkedHashSet<String>()

        // 복수/3인칭 -s / -es / -ies
        if (s.endsWith("s") && s.length >= 4) {
            out.add(s.dropLast(1))                        // cats → cat
            if (s.endsWith("es")) out.add(s.dropLast(2))  // boxes → box
            if (s.endsWith("ies")) out.add(s.dropLast(3) + "y")  // tries → try
        }
        // 과거/과거분사 -ed
        if (s.endsWith("ed") && s.length >= 4) {
            out.add(s.dropLast(2))                        // walked → walk
            out.add(s.dropLast(1))                        // liked → like (묵음 e)
            if (s.endsWith("ied")) out.add(s.dropLast(3) + "y")  // tried → try
            undouble(s.dropLast(2))?.let { out.add(it) }  // stopped → stop
        }
        // 진행 -ing
        if (s.endsWith("ing") && s.length >= 5) {
            val stem = s.dropLast(3)
            out.add(stem)                                 // going → go
            out.add(stem + "e")                           // making → make
            undouble(stem)?.let { out.add(it) }           // running → run
            if (s.endsWith("ying")) out.add(s.dropLast(4) + "ie")  // dying → die
        }
        return out
    }

    /** 겹자음으로 끝나면 한 글자를 떼어 봅니다. (stopp→stop, runn→run) */
    private fun undouble(w: String): String? {
        if (w.length < 3) return null
        val a = w[w.length - 1]
        val b = w[w.length - 2]
        return if (a == b && isConsonant(a)) w.dropLast(1) else null
    }

    // ── 정방향: 원형 → 규칙 굴절형 전체 ──────────────────────────────

    /** 원형 [lemma]의 규칙 굴절형(복수/3인칭, 과거, 진행)을 모읍니다. */
    private fun inflect(lemma: String): Set<String> =
        setOf(pluralS(lemma), pastEd(lemma), ingForm(lemma))

    /** -s / -es / -ies (복수·3인칭 단수) */
    private fun pluralS(w: String): String = when {
        w.endsWith("s") || w.endsWith("x") || w.endsWith("z") ||
            w.endsWith("ch") || w.endsWith("sh") -> w + "es"     // box → boxes
        endsConsonantY(w) -> w.dropLast(1) + "ies"              // try → tries
        endsConsonant(w, 'o') -> w + "es"                       // go → goes
        else -> w + "s"                                         // cat → cats
    }

    /** -ed (과거·과거분사) */
    private fun pastEd(w: String): String = when {
        w.endsWith("e") -> w + "d"                              // like → liked
        endsConsonantY(w) -> w.dropLast(1) + "ied"             // try → tried
        isCVC(w) -> w + w.last() + "ed"                        // stop → stopped
        else -> w + "ed"                                       // walk → walked
    }

    /** -ing (진행) */
    private fun ingForm(w: String): String = when {
        w.endsWith("ie") -> w.dropLast(2) + "ying"             // die → dying
        w.endsWith("e") && !w.endsWith("ee") && w.length > 2 -> w.dropLast(1) + "ing"  // make → making
        isCVC(w) -> w + w.last() + "ing"                       // run → running
        else -> w + "ing"                                      // go → going
    }

    // ── 철자 도우미 ──────────────────────────────────────────────────

    private fun isVowel(c: Char): Boolean = c in "aeiou"
    private fun isConsonant(c: Char): Boolean = c in 'a'..'z' && !isVowel(c)

    /** 자음 + 특정 모음/자음으로 끝나는지 (예: try는 자음 r + y). */
    private fun endsConsonantY(w: String): Boolean =
        w.length >= 2 && w.last() == 'y' && isConsonant(w[w.length - 2])

    private fun endsConsonant(w: String, last: Char): Boolean =
        w.length >= 2 && w.last() == last && isConsonant(w[w.length - 2])

    /**
     * 자음-모음-자음(CVC)으로 끝나 마지막 자음을 겹치는 경우인지(대략적 판정).
     * 마지막 자음이 w/x/y면 겹치지 않습니다. (fix→fixed, play→played)
     */
    private fun isCVC(w: String): Boolean {
        if (w.length < 3) return false
        val c1 = w[w.length - 3]
        val v = w[w.length - 2]
        val c2 = w[w.length - 1]
        return isConsonant(c1) && isVowel(v) && isConsonant(c2) && c2 !in "wxy"
    }

    // ── 고빈도 불규칙 표 (활용형 → 원형, 수동 등록) ──────────────────

    private val IRREGULAR: Map<String, String> = buildMap {
        // be / have / do
        for (w in listOf("was", "were", "been", "being")) put(w, "be")
        for (w in listOf("had", "has", "having")) put(w, "have")
        for (w in listOf("did", "does", "done", "doing")) put(w, "do")
        // 초고빈도 불규칙 동사 (과거/과거분사 → 원형)
        putAll(
            mapOf(
                "went" to "go", "gone" to "go",
                "ate" to "eat", "eaten" to "eat",
                "made" to "make",
                "took" to "take", "taken" to "take",
                "came" to "come", "become" to "become", "became" to "become",
                "got" to "get", "gotten" to "get",
                "gave" to "give", "given" to "give",
                "knew" to "know", "known" to "know",
                "saw" to "see", "seen" to "see",
                "said" to "say",
                "told" to "tell",
                "found" to "find",
                "thought" to "think",
                "brought" to "bring",
                "bought" to "buy",
                "caught" to "catch",
                "taught" to "teach",
                "sold" to "sell",
                "felt" to "feel",
                "kept" to "keep",
                "left" to "leave",
                "met" to "meet",
                "paid" to "pay",
                "sent" to "send",
                "built" to "build",
                "spent" to "spend",
                "lost" to "lose",
                "meant" to "mean",
                "sat" to "sit",
                "stood" to "stand",
                "understood" to "understand",
                "won" to "win",
                "held" to "hold",
                "heard" to "hear",
                "led" to "lead",
                "fed" to "feed",
                "wrote" to "write", "written" to "write",
                "drove" to "drive", "driven" to "drive",
                "rode" to "ride", "ridden" to "ride",
                "rose" to "rise", "risen" to "rise",
                "chose" to "choose", "chosen" to "choose",
                "spoke" to "speak", "spoken" to "speak",
                "broke" to "break", "broken" to "break",
                "woke" to "wake", "woken" to "wake",
                "stole" to "steal", "stolen" to "steal",
                "froze" to "freeze", "frozen" to "freeze",
                "threw" to "throw", "thrown" to "throw",
                "grew" to "grow", "grown" to "grow",
                "flew" to "fly", "flown" to "fly",
                "drew" to "draw", "drawn" to "draw",
                "blew" to "blow", "blown" to "blow",
                "began" to "begin", "begun" to "begin",
                "drank" to "drink", "drunk" to "drink",
                "sang" to "sing", "sung" to "sing",
                "swam" to "swim", "swum" to "swim",
                "rang" to "ring", "rung" to "ring",
                "ran" to "run",
                "swore" to "swear", "sworn" to "swear",
                "tore" to "tear", "torn" to "tear",
                "wore" to "wear", "worn" to "wear",
                "hung" to "hang",
                "dug" to "dig",
                "stuck" to "stick",
                "struck" to "strike",
                "fought" to "fight",
                "sought" to "seek",
                "bit" to "bite", "bitten" to "bite",
                "hid" to "hide", "hidden" to "hide",
                "slept" to "sleep",
                "shot" to "shoot",
                "forgot" to "forget", "forgotten" to "forget",
                "laid" to "lay",
                "spread" to "spread"
            )
        )
        // 불규칙 복수 (복수형 → 단수 원형)
        putAll(
            mapOf(
                "children" to "child",
                "men" to "man", "women" to "woman",
                "feet" to "foot", "teeth" to "tooth",
                "mice" to "mouse", "geese" to "goose",
                "people" to "person",
                "lives" to "life", "wives" to "wife", "knives" to "knife",
                "leaves" to "leaf", "wolves" to "wolf", "shelves" to "shelf",
                "halves" to "half", "thieves" to "thief", "selves" to "self"
            )
        )
    }
}
