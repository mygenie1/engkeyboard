package com.hanyeong.keyboard.dict

/**
 * 용언(동사·형용사) 활용형에서 사전형(원형, "…다")을 복원합니다.
 *
 * 예) "먹었어" → "먹다",  "추워" → "춥다",  "몰라" → "모르다"
 *
 * 이 파일에는 안드로이드 코드가 하나도 없습니다(순수 코틀린). 그래서
 * 단위 테스트에서 "엉뚱한 원형으로 복원되지 않는지"를 통째로 검증할 수 있습니다.
 *
 * ── 안전 제1원칙 ──────────────────────────────────────────────────────
 *   "틀린 추천이 빈 추천보다 나쁘다."
 *   그래서 확신할 수 있을 때만 원형 하나를 돌려주고, 애매하면 null을 돌려줍니다.
 *
 * ── 어떻게 확신하는가 (왕복 검증) ─────────────────────────────────────
 *   활용형에서 원형 후보를 거꾸로 뽑아낸 뒤, 그 원형을 '다시 활용'시켜
 *   입력과 글자 하나까지 똑같아질 때만 인정합니다. 이렇게 하면
 *   잘못 추측한 원형은 활용 결과가 어긋나 저절로 걸러집니다.
 *   게다가 후보 원형은 반드시 '사전에 실제로 있는' 단어여야 합니다.
 *   서로 다른 원형이 2개 이상 나오면(예: "걸어"=걷다/걸다) 미추천합니다.
 *
 * ── 규칙 활용 vs 불규칙 활용 ─────────────────────────────────────────
 *   - 규칙 활용(먹다, 가다, 예쁘다 …)은 어미 결합 규칙을 코드로 구현했습니다.
 *   - 불규칙 활용(듣다, 춥다, 낫다 …)은 초고빈도 용언만 골라 아래
 *     [IRREGULAR] 표에 원형과 불규칙 종류를 손으로 등록했습니다. 이 원형들의
 *     활용형은 미리 만들어 두었다가(IRREGULAR_FORMS) 표에서 바로 찾습니다.
 */
object Conjugation {

    /**
     * 활용형 [surface]의 사전형(원형)을 찾습니다.
     *
     * @param inDict  후보 원형이 사전에 있는지 확인하는 함수 (원형 문자열 → 있음?)
     * @return 확신할 수 있는 원형 하나. 후보가 없거나 2개 이상이면 null.
     */
    fun resolveBase(surface: String, inDict: (String) -> Boolean): String? {
        // 한 글자짜리(가/봐/해 …)는 명령·감탄 등과 겹쳐 너무 애매합니다 → 시도 안 함.
        if (surface.length < 2) return null
        // 완성된 한글 음절로만 이뤄져야 합니다. (자음/모음 낱자, 영문·기호 섞이면 제외)
        if (surface.any { decompose(it) == null }) return null
        // 활용형처럼 생겼지만 실제로는 접속·부사(하지만, 그리고 …)인 말들은 제외.
        if (surface in STOPWORDS) return null

        val candidates = LinkedHashSet<String>()

        // 1) 규칙 어미를 거꾸로 벗겨 원형 후보를 만들고, '다시 활용'시켜 검증.
        for (base in reverseCandidateBases(surface)) {
            if (!inDict(base)) continue
            val type = IRREGULAR[base] ?: Irr.REGULAR
            if (surface in allForms(base, type)) candidates.add(base)
        }
        // 2) 불규칙 활용형은 미리 만들어 둔 표에서 바로 찾습니다.
        IRREGULAR_FORMS[surface]?.forEach { base -> if (inDict(base)) candidates.add(base) }

        // 서로 다른 원형이 정확히 하나일 때만 확신합니다.
        return if (candidates.size == 1) candidates.first() else null
    }

    // ── 불규칙 용언 등록표 (초고빈도만 손으로 등록) ──────────────────────

    /** 불규칙 활용의 종류. */
    private enum class Irr { REGULAR, D, B, S, REU, H }

    /**
     * 원형 → 불규칙 종류.
     *  D  = ㄷ 불규칙 (듣다: 들어)      B  = ㅂ 불규칙 (춥다: 추워)
     *  S  = ㅅ 불규칙 (낫다: 나아)      REU= 르 불규칙 (모르다: 몰라)
     *  H  = ㅎ 불규칙 (그렇다: 그래)
     *  (하다류 '여 불규칙'은 규칙 처리 안에서 '하 → 해'로 따로 다룹니다.)
     */
    private val IRREGULAR: Map<String, Irr> = buildMap {
        for (w in listOf("듣다", "걷다", "묻다", "싣다", "깨닫다", "붇다")) put(w, Irr.D)
        for (w in listOf(
            "춥다", "덥다", "돕다", "곱다", "굽다", "눕다", "줍다", "맵다", "쉽다",
            "어렵다", "무겁다", "가볍다", "반갑다", "고맙다", "즐겁다", "아름답다",
            "차갑다", "뜨겁다", "두껍다", "싱겁다", "귀엽다", "더럽다", "밉다"
        )) put(w, Irr.B)
        for (w in listOf("낫다", "짓다", "붓다", "젓다", "긋다")) put(w, Irr.S)
        for (w in listOf(
            "모르다", "부르다", "고르다", "다르다", "빠르다", "기르다", "흐르다",
            "오르다", "자르다", "누르다", "마르다", "바르다", "이르다", "지르다",
            "두르다", "가르다", "구르다", "거르다"
        )) put(w, Irr.REU)
        for (w in listOf(
            "그렇다", "어떻다", "이렇다", "저렇다", "빨갛다", "파랗다", "노랗다",
            "하얗다", "까맣다", "동그랗다"
        )) put(w, Irr.H)
    }

    /**
     * 불규칙 원형들의 활용형 → 원형(들). 첫 사용 때 한 번만 만듭니다.
     * (지연 초기화: 아래 어미 목록 등 다른 프로퍼티가 모두 준비된 뒤 계산되도록.)
     */
    private val IRREGULAR_FORMS: Map<String, Set<String>> by lazy {
        buildMap<String, MutableSet<String>> {
            for ((base, type) in IRREGULAR) {
                for (form in allForms(base, type)) {
                    getOrPut(form) { LinkedHashSet() }.add(base)
                }
            }
        }
    }

    /**
     * 활용형처럼 생겼지만 원형 추천을 띄우면 안 되는 말들(접속사·부사 등).
     * 예) "그리고"는 형태만 보면 '그리다+고'지만 실제로는 접속사입니다.
     */
    private val STOPWORDS: Set<String> = setOf(
        "하지만", "그리고", "그래서", "그래", "그러나", "그런데",
        "그러면", "그러니까", "그렇지만", "아니", "그러다"
    )

    // ── 어미 목록 ────────────────────────────────────────────────────

    // 자음으로 시작해 어간에 바로 붙는 어미(규칙 용언은 어간이 안 바뀜).
    // ※ '자/게/지' 등은 명사(과자·의자·바지…)와 잘 겹쳐 오인식 위험이 커 뺐습니다.
    private val CONSONANT_ENDINGS = listOf("고", "지만", "네", "니")

    // 현재형 '아/어' 어미 뒤에 오는 꼬리. ""=반말(먹어), 요=먹어요, 서=먹어서.
    private val PRESENT_TAILS = listOf("", "요", "서")

    // 과거 '았/었' 뒤에 오는 꼬리. 었어/었다/었어요/었지만/었고/었네/었니.
    private val PAST_TAILS = listOf("어", "다", "어요", "지만", "고", "네", "니")

    // ── 정방향: 원형 → 활용형 전체 ───────────────────────────────────

    /** 원형 [base]가 만들 수 있는 (우리가 다루는) 활용형 전체를 모읍니다. */
    private fun allForms(base: String, type: Irr): Set<String> {
        val stem = base.dropLast(1)                // "먹다" → "먹"
        if (stem.isEmpty()) return emptySet()
        val out = LinkedHashSet<String>()
        for (e in CONSONANT_ENDINGS) out.add(stem + e)   // 어간에 그대로 붙는 어미
        val eo = applyEo(stem, type, base) ?: return out  // '아/어' 결합형 (먹어, 추워 …)
        for (t in PRESENT_TAILS) out.add(eo + t)
        val past = addSsatchim(eo)                        // 과거 '았/었' = 결합형에 ㅆ받침
        if (past != null) for (t in PAST_TAILS) out.add(past + t)
        return out
    }

    /** 어간에 '아/어'를 결합한 형태를 만듭니다. (불규칙 종류에 따라 다르게) */
    private fun applyEo(stem: String, type: Irr, base: String): String? = when (type) {
        Irr.REGULAR -> applyEoRegular(stem)
        Irr.D -> applyEoD(stem)
        Irr.B -> applyEoB(stem, base)
        Irr.S -> applyEoS(stem)
        Irr.REU -> applyEoReu(stem)
        Irr.H -> applyEoH(stem)
    }

    /** 규칙 용언의 '아/어' 결합. (하다류는 '하→해'로 특별 처리) */
    private fun applyEoRegular(stem: String): String? {
        val last = stem.last()
        if (last == '하') return stem.dropLast(1) + '해'   // 공부하 → 공부해
        val (cho, jung, jong) = decompose(last) ?: return null
        if (jong != 0) return stem + if (isBright(jung)) "아" else "어"  // 받침 있음: 먹→먹어
        // 모음으로 끝나는 어간: 모음끼리 줄어듭니다.
        return when (jung) {
            J_A -> stem                                 // ㅏ+아 → ㅏ (가)
            J_EO -> stem                                // ㅓ+어 → ㅓ (서)
            J_AE, J_E -> stem                           // ㅐ/ㅔ + 어 → 그대로 (내, 세)
            J_O -> stem.dropLast(1) + compose(cho, J_WA, 0)   // ㅗ → ㅘ (봐)
            J_U -> stem.dropLast(1) + compose(cho, J_WEO, 0)  // ㅜ → ㅝ (줘)
            J_OE -> stem.dropLast(1) + compose(cho, J_WAE, 0) // ㅚ → ㅙ (돼)
            J_I -> stem.dropLast(1) + compose(cho, J_YEO, 0)  // ㅣ → ㅕ (마셔)
            J_WI -> stem + "어"                          // ㅟ + 어 → 쉬어 (안 줄어듦)
            J_EU -> {                                    // ㅡ 탈락: 크→커, 아프→아파
                val bright = stem.length >= 2 &&
                    decompose(stem[stem.length - 2])?.let { isBright(it.second) } == true
                stem.dropLast(1) + compose(cho, if (bright) J_A else J_EO, 0)
            }
            else -> null
        }
    }

    /** ㄷ 불규칙: 어간 끝 ㄷ → ㄹ. 듣→들어 */
    private fun applyEoD(stem: String): String? {
        val (cho, jung, jong) = decompose(stem.last()) ?: return null
        if (jong != JONG_D) return null
        val changed = compose(cho, jung, JONG_L)
        return stem.dropLast(1) + changed + if (isBright(jung)) "아" else "어"
    }

    /** ㅂ 불규칙: 어간 끝 ㅂ → 우/오. 춥→추워, 돕→도와 */
    private fun applyEoB(stem: String, base: String): String? {
        val (cho, jung, jong) = decompose(stem.last()) ?: return null
        if (jong != JONG_B) return null
        val dropped = compose(cho, jung, 0)                 // ㅂ 떼기: 춥→추, 돕→도
        val tail = if (base == "돕다" || base == "곱다") "와" else "워"
        return stem.dropLast(1) + dropped + tail
    }

    /** ㅅ 불규칙: 어간 끝 ㅅ 탈락(모음은 안 줄어듦). 낫→나아, 짓→지어 */
    private fun applyEoS(stem: String): String? {
        val (cho, jung, jong) = decompose(stem.last()) ?: return null
        if (jong != JONG_S) return null
        val dropped = compose(cho, jung, 0)
        return stem.dropLast(1) + dropped + if (isBright(jung)) "아" else "어"
    }

    /** 르 불규칙: 앞 음절에 ㄹ받침이 붙고 '르'는 라/러로. 모르→몰라, 부르→불러 */
    private fun applyEoReu(stem: String): String? {
        if (stem.length < 2 || stem.last() != '르') return null
        val prev = decompose(stem[stem.length - 2]) ?: return null
        val newPrev = compose(prev.first, prev.second, JONG_L)   // 앞 음절 + ㄹ받침
        val tail = if (isBright(prev.second)) "라" else "러"
        return stem.dropLast(2) + newPrev + tail
    }

    /** ㅎ 불규칙(형용사): 어간 끝 ㅎ 탈락 + 모음이 ㅐ/ㅒ로. 그렇→그래, 빨갛→빨개 */
    private fun applyEoH(stem: String): String? {
        val (cho, jung, jong) = decompose(stem.last()) ?: return null
        if (jong != JONG_H) return null
        val newJung = when (jung) {
            J_A, J_EO -> J_AE     // ㅏ/ㅓ → ㅐ
            J_YA -> J_YAE         // ㅑ → ㅒ (하얗→하얘)
            else -> return null
        }
        return stem.dropLast(1) + compose(cho, newJung, 0)
    }

    // ── 역방향: 활용형 → 원형 후보(과다 생성; 정방향 검증으로 걸러짐) ────

    /** 활용형에서 규칙 어미를 벗겨 원형 후보("…다")들을 넉넉히 만듭니다. */
    private fun reverseCandidateBases(surface: String): Set<String> {
        val stems = LinkedHashSet<String>()

        for (e in CONSONANT_ENDINGS) {
            if (surface.length > e.length && surface.endsWith(e)) {
                stems.add(surface.substring(0, surface.length - e.length))
            }
        }
        for (t in PRESENT_TAILS) {
            if (surface.length > t.length && surface.endsWith(t)) {
                stems.addAll(unApplyEo(surface.substring(0, surface.length - t.length)))
            }
        }
        for (t in PAST_TAILS) {
            if (surface.length > t.length && surface.endsWith(t)) {
                val core = surface.substring(0, surface.length - t.length)  // …았/었 로 끝남
                val depast = removeSsatchim(core) ?: continue               // ㅆ받침 떼기
                stems.addAll(unApplyEo(depast))
            }
        }
        return stems.filter { it.isNotEmpty() }.map { it + "다" }.toSet()
    }

    /** '아/어' 결합형에서 가능한 어간 후보들을 되돌립니다. (여러 개일 수 있음) */
    private fun unApplyEo(s: String): List<String> {
        if (s.isEmpty()) return emptyList()
        val last = s.last()
        val (cho, jung, jong) = decompose(last) ?: return emptyList()
        val out = ArrayList<String>()

        // (가) '아/어'가 제 음절로 붙은 경우(받침 있는 어간): 먹어 → 먹
        if (cho == CHO_IEUNG && jong == 0 && (jung == J_A || jung == J_EO) && s.length >= 2) {
            out.add(s.dropLast(1))
        }
        // (나) 하다류: 해 → 하
        if (last == '해') out.add(s.dropLast(1) + '하')
        // (다) 모음이 줄어든 경우: 마지막 음절을 원래 어간 모음으로 되돌립니다.
        if (jong == 0) {
            val stemJungs = when (jung) {
                J_A -> intArrayOf(J_A, J_EU)     // ㅏ ← ㅏ(가) / ㅡ(아파)
                J_EO -> intArrayOf(J_EO, J_EU)   // ㅓ ← ㅓ(서) / ㅡ(커)
                J_WA -> intArrayOf(J_O)          // ㅘ ← ㅗ(봐)
                J_WEO -> intArrayOf(J_U)         // ㅝ ← ㅜ(줘)
                J_YEO -> intArrayOf(J_I)         // ㅕ ← ㅣ(마셔)
                J_WAE -> intArrayOf(J_OE)        // ㅙ ← ㅚ(돼)
                J_AE -> intArrayOf(J_AE)         // ㅐ ← ㅐ(내)
                J_E -> intArrayOf(J_E)           // ㅔ ← ㅔ(세)
                J_WI -> intArrayOf(J_WI)         // ㅟ ← ㅟ(쉬)
                else -> IntArray(0)
            }
            for (sj in stemJungs) out.add(s.dropLast(1) + compose(cho, sj, 0))
        }
        return out
    }

    // ── 음절 조립/분해 도우미 ────────────────────────────────────────

    /** 완성 한글 음절을 (초성번호, 중성번호, 종성번호)로 분해합니다. 음절이 아니면 null. */
    private fun decompose(c: Char): Triple<Int, Int, Int>? {
        val idx = c.code - 0xAC00
        if (idx !in 0 until 11172) return null
        return Triple(idx / (21 * 28), (idx % (21 * 28)) / 28, idx % 28)
    }

    /** (초성번호, 중성번호, 종성번호)로 완성 한글 음절을 만듭니다. */
    private fun compose(cho: Int, jung: Int, jong: Int): Char =
        (0xAC00 + (cho * 21 + jung) * 28 + jong).toChar()

    /** 마지막 음절에 ㅆ받침을 더합니다.(과거형) 받침이 이미 있으면 null. */
    private fun addSsatchim(s: String): String? {
        if (s.isEmpty()) return null
        val (cho, jung, jong) = decompose(s.last()) ?: return null
        if (jong != 0) return null
        return s.dropLast(1) + compose(cho, jung, JONG_SS)
    }

    /** 마지막 음절의 ㅆ받침을 뗍니다. ㅆ받침이 아니면 null. */
    private fun removeSsatchim(s: String): String? {
        if (s.isEmpty()) return null
        val (cho, jung, jong) = decompose(s.last()) ?: return null
        if (jong != JONG_SS) return null
        return s.dropLast(1) + compose(cho, jung, 0)
    }

    /** 양성모음(ㅏ,ㅑ,ㅗ,ㅛ)이면 '아' 계열, 아니면 '어' 계열로 결합합니다. */
    private fun isBright(jung: Int): Boolean =
        jung == J_A || jung == J_YA || jung == J_O || jung == J_YO

    // 중성(모음) 번호 상수 — Tables.JUNG "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ" 순서
    private const val J_A = 0    // ㅏ
    private const val J_AE = 1   // ㅐ
    private const val J_YA = 2   // ㅑ
    private const val J_YAE = 3  // ㅒ
    private const val J_EO = 4   // ㅓ
    private const val J_E = 5    // ㅔ
    private const val J_YEO = 6  // ㅕ
    private const val J_O = 8    // ㅗ
    private const val J_WA = 9   // ㅘ
    private const val J_WAE = 10 // ㅙ
    private const val J_OE = 11  // ㅚ
    private const val J_YO = 12  // ㅛ
    private const val J_U = 13   // ㅜ
    private const val J_WEO = 14 // ㅝ
    private const val J_WI = 16  // ㅟ
    private const val J_EU = 18  // ㅡ
    private const val J_I = 20   // ㅣ

    // 초성(자음) 번호 상수 — Tables.CHO "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇ…" 순서
    private const val CHO_IEUNG = 11  // ㅇ

    // 종성(받침) 번호 상수 — Tables.JONG 순서
    private const val JONG_D = 7    // ㄷ
    private const val JONG_L = 8    // ㄹ
    private const val JONG_B = 17   // ㅂ
    private const val JONG_S = 19   // ㅅ
    private const val JONG_SS = 20  // ㅆ
    private const val JONG_H = 27   // ㅎ
}
