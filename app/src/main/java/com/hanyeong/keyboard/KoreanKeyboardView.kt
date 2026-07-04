package com.hanyeong.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.hanyeong.keyboard.dict.DictEntry

/**
 * 자판(한글 두벌식 · 영어 QWERTY · 숫자/기호) + 추천 바 + 학습 카드를
 * '직접 그리는' 화면입니다.
 *
 * 세로 구조:
 *   [추천 바]  ← 항상 같은 높이로 자리를 차지 (비어 있어도) → 나타나도 화면이 안 밀림
 *   [본체]     ← 자판 + 그 위에 겹쳐지는 학습 카드
 *
 * 자판은 3가지 모드로 갈아끼워집니다:
 *   - 한글(KOREAN)  : 두벌식 자모
 *   - 영어(ENGLISH) : QWERTY (Shift 대문자 · 더블탭 Caps Lock)
 *   - 기호(SYMBOL)  : 숫자·기호판 (2쪽 구성, '?123'/'ABC·가' 키로 왕복)
 *
 * 학습 카드는 자판 위에 '겹쳐서' 열립니다. 전체 높이가 변하지 않으므로
 * 카드가 열리고 닫혀도 입력창이 위아래로 출렁이지 않습니다.
 */
@SuppressLint("ViewConstructor")
class KoreanKeyboardView(context: Context) : LinearLayout(context) {

    /** 키를 눌렀을 때 키보드 본체(Service)에 알려 주는 통로입니다. */
    interface Listener {
        fun onJamo(jamo: Char)   // 한글 자모 키(ㄱ, ㅏ ...)
        fun onBackspace()        // 지우기
        fun onSpace()            // 띄어쓰기
        fun onEnter()            // 줄바꿈/전송
        fun onText(text: String) // 영문/숫자/기호 등 그대로 확정되는 글자
        /**
         * 한/영·기호판 전환 직전에 호출됩니다.
         * 조합 중이던 한글을 확정해 글자가 사라지지 않게 하는 것이 목적입니다.
         */
        fun onModeSwitch()
    }

    var listener: Listener? = null

    /** 지금 어떤 자판을 보여 주는가. */
    private enum class Mode { KOREAN, ENGLISH, SYMBOL }
    private var mode = Mode.KOREAN
    /** 기호판에서 'ABC·가'를 눌렀을 때 돌아갈 글자 자판. */
    private var lastLetterMode = Mode.KOREAN
    /** 기호판 쪽 번호 (0 = 기본 기호, 1 = 추가 기호). */
    private var symbolPage = 0

    // Shift 상태 (한글=쌍자음, 영어=대문자)
    private var shifted = false
    // Caps Lock (영어 전용): 켜지면 Shift가 계속 유지됨
    private var capsLock = false
    private var lastShiftTapTime = 0L

    // 한글 자모 키들 (Shift로 쌍자음 토글)
    private val charKeys = mutableListOf<Pair<TextView, CharKey>>()
    private data class CharKey(val normal: String, val shifted: String? = null)

    // 영어 글자 키들 (Shift/Caps로 대소문자 토글) — 소문자를 기준값으로 보관
    private val letterKeys = mutableListOf<Pair<TextView, String>>()
    // 현재 화면의 Shift 키 (모드 새로 그릴 때마다 갱신)
    private var shiftKey: TextView? = null

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val isLandscape =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 키/바 높이: 가로 모드에서는 더 낮게. 설정의 '키보드 높이'(낮음/보통/높음)를 곱함.
    private val heightScale = Settings.heightScale(context)
    private val charRowH = (if (isLandscape) 34f else 52f) * heightScale
    private val numRowH = (if (isLandscape) 28f else 44f) * heightScale
    private val suggestionBarH = if (isLandscape) 34f else 46f   // 추천 바는 고정 높이

    // 키 입력 피드백(소리/진동) 설정값을 만들 때 한 번 읽어 둡니다.
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val soundOn = Settings.soundEnabled(context)
    private val vibrateOn = Settings.vibrationEnabled(context)

    private val repeatHandler = Handler(Looper.getMainLooper())

    // 색상
    private val colorBoard = Color.parseColor("#D5D8DE")
    private val colorKey = Color.parseColor("#FFFFFF")
    private val colorKeyPressed = Color.parseColor("#C7CBD1")
    private val colorSpecial = Color.parseColor("#B9BEC7")
    private val colorSpecialPressed = Color.parseColor("#9AA0AB")
    private val colorText = Color.parseColor("#1C1E21")
    private val colorAccent = Color.parseColor("#1A5FB4")       // 활성 Shift 배경
    private val colorAccentPressed = Color.parseColor("#154C90")
    private val colorChip = Color.parseColor("#FFFFFF")
    private val colorChipText = Color.parseColor("#1A5FB4")
    private val colorCardBg = Color.parseColor("#FFFFFF")
    private val colorMeaning = Color.parseColor("#374151")
    private val colorSub = Color.parseColor("#6B7280")

    // 추천 바와 학습 카드의 부품들
    private lateinit var suggestionBar: LinearLayout
    private lateinit var keysContainer: LinearLayout   // 자판 본체(모드마다 다시 그림)
    private lateinit var cardView: LinearLayout
    private lateinit var cardEnglish: TextView
    private lateinit var cardPron: TextView
    private lateinit var cardMeaning: TextView
    private lateinit var cardExample: TextView

    // 영어 QWERTY 배열
    private val enRow1 = "qwertyuiop"
    private val enRow2 = "asdfghjkl"
    private val enRow3 = "zxcvbnm"

    // 숫자/기호판 배열.
    // 글자 자판과 높이를 맞추기 위해 각 쪽은 3줄(10칸·10칸·7칸)로 구성합니다.
    // 마지막 7칸 줄은 왼쪽 '기호 더보기'와 오른쪽 백스페이스 사이에 놓입니다.
    private val symbolPages = listOf(
        // 0쪽: 상용 기호 (요청된 표준 세트 !?@#$%&*()-+= 를 우선 배치)
        listOf(
            listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
            listOf("=", "_", "/", "\\", ":", ";", "!", "?", "'", "\""),
            listOf("<", ">", "[", "]", "{", "}", "•")
        ),
        // 1쪽: 추가 기호 (화폐/수학/특수)
        listOf(
            listOf("~", "`", "|", "^", "°", "…", "§", "¶", "™", "•"),
            listOf("£", "€", "¥", "₩", "¢", "©", "®", "±", "×", "÷"),
            listOf("√", "π", "≈", "≠", "≤", "≥", "∞")
        )
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(colorBoard)

        // 1) 추천 바 (항상 표시, 높이 고정)
        suggestionBar = buildSuggestionBar()
        addView(
            suggestionBar,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(suggestionBarH))
        )

        // 2) 본체: 자판 + 카드(겹침)
        val body = FrameLayout(context)

        keysContainer = LinearLayout(context)
        keysContainer.orientation = VERTICAL
        val vPad = if (isLandscape) 3f else 6f
        keysContainer.setPadding(dp(3f), dp(vPad), dp(3f), dp(vPad + 2f))
        body.addView(
            keysContainer,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        )

        // 학습 카드: 처음엔 숨김, 열리면 자판을 덮음
        cardView = buildCardView()
        cardView.visibility = GONE
        body.addView(
            cardView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        addView(body, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        rebuildKeys()   // 처음엔 한글 자판
    }

    // ── 자판 모드 전환 ──────────────────────────────────────────────

    /** 자판 본체를 현재 모드에 맞게 다시 그립니다. */
    private fun rebuildKeys() {
        keysContainer.removeAllViews()
        charKeys.clear()
        letterKeys.clear()
        shiftKey = null

        keysContainer.addView(buildNumberRow())
        when (mode) {
            Mode.KOREAN -> buildKoreanRows()
            Mode.ENGLISH -> buildEnglishRows()
            Mode.SYMBOL -> buildSymbolRows()
        }
        keysContainer.addView(buildBottomRow())
    }

    /** 한/영 전환. 조합 중 한글을 먼저 확정한 뒤 자판을 바꿉니다. */
    private fun toggleLanguage() {
        listener?.onModeSwitch()
        mode = if (mode == Mode.KOREAN) Mode.ENGLISH else Mode.KOREAN
        lastLetterMode = mode
        shifted = false
        capsLock = false
        rebuildKeys()
    }

    /** 글자판 ↔ 기호판 전환. 조합 중 한글을 먼저 확정합니다. */
    private fun toggleSymbols() {
        listener?.onModeSwitch()
        if (mode == Mode.SYMBOL) {
            mode = lastLetterMode          // 원래 쓰던 글자판(한/영)으로 복귀
        } else {
            lastLetterMode = mode          // 지금 글자판을 기억해 두고
            mode = Mode.SYMBOL
            symbolPage = 0
        }
        shifted = false
        capsLock = false
        rebuildKeys()
    }

    private fun toggleSymbolPage() {
        symbolPage = if (symbolPage == 0) 1 else 0
        rebuildKeys()
    }

    // ── 추천 바 ─────────────────────────────────────────────────────

    private fun buildSuggestionBar(): LinearLayout {
        val bar = LinearLayout(context)
        bar.orientation = HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
        return bar
    }

    /**
     * 추천 단어들을 추천 바에 표시합니다. (빈 목록이면 바를 비웁니다)
     * Service가 '단어를 다 쳤을 때' 호출합니다. (영어/기호 모드에서는 항상 비어 있음)
     */
    fun showSuggestions(entries: List<DictEntry>) {
        suggestionBar.removeAllViews()
        for (e in entries) {
            val chip = makeChip(e.english)
            chip.setOnClickListener { openCard(e) }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(if (isLandscape) 26f else 34f))
            lp.marginEnd = dp(6f)
            suggestionBar.addView(chip, lp)
        }
    }

    private fun makeChip(english: String): TextView {
        val tv = TextView(context)
        tv.text = english
        tv.gravity = Gravity.CENTER
        tv.setTextColor(colorChipText)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        tv.setPadding(dp(14f), 0, dp(14f), 0)
        tv.isClickable = true
        tv.background = keyBackground(colorChip, colorKeyPressed)
        return tv
    }

    // ── 학습 카드 ───────────────────────────────────────────────────

    private fun buildCardView(): LinearLayout {
        val card = LinearLayout(context)
        card.orientation = VERTICAL
        card.setBackgroundColor(colorCardBg)
        card.setPadding(dp(18f), dp(14f), dp(18f), dp(14f))
        // 카드는 자판을 덮는 불투명 판입니다. 아무 곳이나 누르면 닫혀,
        // 다시 타이핑을 시작할 수 있습니다.
        card.isClickable = true
        card.setOnClickListener { hideCard() }

        // 윗줄: 영어 단어 + 발음기호 + 닫기(✕)
        val top = LinearLayout(context)
        top.orientation = HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL

        cardEnglish = TextView(context)
        cardEnglish.setTextColor(colorText)
        cardEnglish.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        cardEnglish.setTypeface(cardEnglish.typeface, android.graphics.Typeface.BOLD)
        top.addView(cardEnglish)

        cardPron = TextView(context)
        cardPron.setTextColor(colorSub)
        cardPron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        val pronLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        pronLp.marginStart = dp(10f)
        top.addView(cardPron, pronLp)

        val spacer = View(context)
        top.addView(spacer, LayoutParams(0, 1, 1f))

        val close = TextView(context)
        close.text = "✕"
        close.setTextColor(colorSub)
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        close.setPadding(dp(8f), dp(2f), dp(4f), dp(2f))
        close.isClickable = true
        close.setOnClickListener { hideCard() }
        top.addView(close)

        card.addView(top)

        // 뜻
        cardMeaning = TextView(context)
        cardMeaning.setTextColor(colorMeaning)
        cardMeaning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        val meaningLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        meaningLp.topMargin = dp(10f)
        card.addView(cardMeaning, meaningLp)

        // 예문
        cardExample = TextView(context)
        cardExample.setTextColor(colorSub)
        cardExample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        val exLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        exLp.topMargin = dp(6f)
        card.addView(cardExample, exLp)

        return card
    }

    private fun openCard(e: DictEntry) {
        cardEnglish.text = e.english
        // 2군(간이 카드)은 발음기호·예문이 없습니다. 빈 칸은 아예 숨겨서
        // 카드가 휑하게 비어 보이지 않도록 합니다.
        setOptionalLine(cardPron, e.pronunciation, "")
        setOptionalLine(cardMeaning, e.meaning, "뜻 · ")
        setOptionalLine(cardExample, e.example, "예문 · ")
        cardView.visibility = VISIBLE
    }

    /** 값이 있으면 접두어를 붙여 보여 주고, 비어 있으면 그 줄을 숨깁니다. */
    private fun setOptionalLine(tv: TextView, value: String, prefix: String) {
        if (value.isBlank()) {
            tv.visibility = GONE
        } else {
            tv.visibility = VISIBLE
            tv.text = prefix + value
        }
    }

    /** 학습 카드를 닫습니다. (다시 타이핑을 시작하면 호출됨) */
    fun hideCard() {
        if (cardView.visibility != GONE) cardView.visibility = GONE
    }

    // ── 공통 줄 ─────────────────────────────────────────────────────

    private fun buildNumberRow(): View {
        val row = rowContainer(numRowH)
        for (n in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) {
            val tv = makeKey(n, colorKey, colorKeyPressed)
            tv.setOnClickListener { listener?.onText(n) }
            row.addView(tv, keyParams(1f))
        }
        return row
    }

    /**
     * 맨 아랫줄. 모드에 따라 왼쪽 키가 달라집니다.
     *   글자판: [?123] [,] [한/영] [스페이스] [.] [⏎]
     *   기호판: [ABC·가] [,] [스페이스] [.] [⏎]
     */
    private fun buildBottomRow(): View {
        val row = rowContainer(charRowH)

        if (mode == Mode.SYMBOL) {
            val back = makeKey(if (lastLetterMode == Mode.KOREAN) "가" else "ABC",
                colorSpecial, colorSpecialPressed)
            back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            back.setOnClickListener { toggleSymbols() }
            row.addView(back, keyParams(2f))

            addComma(row, 1f)
            addSpace(row, 5f)
            addPeriod(row, 1f)
            addEnter(row, 2f)
        } else {
            val sym = makeKey("?123", colorSpecial, colorSpecialPressed)
            sym.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            sym.setOnClickListener { toggleSymbols() }
            row.addView(sym, keyParams(1.5f))

            addComma(row, 1f)

            val lang = makeKey("한/영", colorSpecial, colorSpecialPressed)
            lang.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            lang.setOnClickListener { toggleLanguage() }
            row.addView(lang, keyParams(1.5f))

            addSpace(row, 4f)
            addPeriod(row, 1f)
            addEnter(row, 2f)
        }
        return row
    }

    private fun addComma(row: LinearLayout, weight: Float) {
        val comma = makeKey(",", colorSpecial, colorSpecialPressed)
        comma.setOnClickListener { listener?.onText(",") }
        row.addView(comma, keyParams(weight))
    }

    private fun addPeriod(row: LinearLayout, weight: Float) {
        val period = makeKey(".", colorSpecial, colorSpecialPressed)
        period.setOnClickListener { listener?.onText(".") }
        row.addView(period, keyParams(weight))
    }

    private fun addSpace(row: LinearLayout, weight: Float) {
        val space = makeKey("", colorKey, colorKeyPressed)
        space.setOnClickListener { listener?.onSpace() }
        row.addView(space, keyParams(weight))
    }

    private fun addEnter(row: LinearLayout, weight: Float) {
        val enter = makeKey("⏎", colorSpecial, colorSpecialPressed)
        enter.setOnClickListener { listener?.onEnter() }
        row.addView(enter, keyParams(weight))
    }

    /** 왼쪽 특수키(Shift/기호더보기) + 가운데 키들 + 오른쪽 백스페이스 형태의 줄. */
    private fun addBackspace(row: LinearLayout, weight: Float) {
        val back = makeKey("⌫", colorSpecial, colorSpecialPressed)
        attachRepeatingTouch(back) { listener?.onBackspace() }
        row.addView(back, keyParams(weight))
    }

    // ── 한글 자판 ───────────────────────────────────────────────────

    private fun buildKoreanRows() {
        keysContainer.addView(buildCharRow(listOf(
            CharKey("ㅂ", "ㅃ"), CharKey("ㅈ", "ㅉ"), CharKey("ㄷ", "ㄸ"),
            CharKey("ㄱ", "ㄲ"), CharKey("ㅅ", "ㅆ"),
            CharKey("ㅛ"), CharKey("ㅕ"), CharKey("ㅑ"),
            CharKey("ㅐ", "ㅒ"), CharKey("ㅔ", "ㅖ")
        )))
        keysContainer.addView(buildCharRow(
            listOf(
                CharKey("ㅁ"), CharKey("ㄴ"), CharKey("ㅇ"), CharKey("ㄹ"), CharKey("ㅎ"),
                CharKey("ㅗ"), CharKey("ㅓ"), CharKey("ㅏ"), CharKey("ㅣ")
            ),
            sideSpacerWeight = 0.5f
        ))

        // 3번째 줄: Shift + 자모 7개 + 백스페이스
        val row = rowContainer(charRowH)
        val shift = makeKey("⇧", colorSpecial, colorSpecialPressed)
        shift.setOnClickListener { onShiftKey() }
        shiftKey = shift
        row.addView(shift, keyParams(1.5f))
        for (s in listOf("ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ")) {
            val k = CharKey(s)
            val tv = makeKey(s, colorKey, colorKeyPressed)
            tv.setOnClickListener { onCharKeyTapped(k) }
            charKeys.add(tv to k)
            row.addView(tv, keyParams(1f))
        }
        addBackspace(row, 1.5f)
        keysContainer.addView(row)

        applyShiftAppearance()
    }

    private fun buildCharRow(keys: List<CharKey>, sideSpacerWeight: Float = 0f): View {
        val row = rowContainer(charRowH)
        if (sideSpacerWeight > 0f) row.addView(spacer(sideSpacerWeight))
        for (k in keys) {
            val tv = makeKey(k.normal, colorKey, colorKeyPressed)
            tv.setOnClickListener { onCharKeyTapped(k) }
            charKeys.add(tv to k)
            row.addView(tv, keyParams(1f))
        }
        if (sideSpacerWeight > 0f) row.addView(spacer(sideSpacerWeight))
        return row
    }

    private fun onCharKeyTapped(k: CharKey) {
        val text = if (shifted && k.shifted != null) k.shifted else k.normal
        listener?.onJamo(text[0])
        if (shifted) setKoreanShifted(false) // 한 글자 치면 Shift 자동 해제
    }

    // ── 영어 QWERTY 자판 ────────────────────────────────────────────

    private fun buildEnglishRows() {
        keysContainer.addView(buildLetterRow(enRow1))
        keysContainer.addView(buildLetterRow(enRow2, sideSpacerWeight = 0.5f))

        // 3번째 줄: Shift + z x c v b n m + 백스페이스
        val row = rowContainer(charRowH)
        val shift = makeKey("⇧", colorSpecial, colorSpecialPressed)
        shift.setOnClickListener { onShiftKey() }
        shiftKey = shift
        row.addView(shift, keyParams(1.5f))
        for (c in enRow3) addLetterKey(row, c.toString(), 1f)
        addBackspace(row, 1.5f)
        keysContainer.addView(row)

        applyShiftAppearance()
        refreshLetterCase()
    }

    private fun buildLetterRow(letters: String, sideSpacerWeight: Float = 0f): View {
        val row = rowContainer(charRowH)
        if (sideSpacerWeight > 0f) row.addView(spacer(sideSpacerWeight))
        for (c in letters) addLetterKey(row, c.toString(), 1f)
        if (sideSpacerWeight > 0f) row.addView(spacer(sideSpacerWeight))
        return row
    }

    private fun addLetterKey(row: LinearLayout, base: String, weight: Float) {
        val tv = makeKey(base, colorKey, colorKeyPressed)
        tv.setOnClickListener { onLetterTapped(base) }
        letterKeys.add(tv to base)
        row.addView(tv, keyParams(weight))
    }

    private fun onLetterTapped(base: String) {
        val upper = shifted || capsLock
        listener?.onText(if (upper) base.uppercase() else base)
        // 한 번 켠 Shift(캡스락 아님)는 한 글자 뒤 자동 해제
        if (shifted && !capsLock) {
            shifted = false
            refreshLetterCase()
            applyShiftAppearance()
        }
    }

    /** 영어 키 라벨을 현재 대소문자 상태에 맞게 갱신합니다. */
    private fun refreshLetterCase() {
        val upper = shifted || capsLock
        for ((tv, base) in letterKeys) {
            tv.text = if (upper) base.uppercase() else base
        }
    }

    // ── 숫자/기호판 ─────────────────────────────────────────────────

    private fun buildSymbolRows() {
        val page = symbolPages[symbolPage]

        // 1·2번째 줄: 각 10칸
        keysContainer.addView(buildSymbolKeyRow(page[0]))
        keysContainer.addView(buildSymbolKeyRow(page[1]))

        // 3번째 줄: [기호 더보기(1/2 전환)] + 7칸 + 백스페이스
        val row = rowContainer(charRowH)
        val more = makeKey(if (symbolPage == 0) "1/2" else "2/2", colorSpecial, colorSpecialPressed)
        more.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        more.setOnClickListener { toggleSymbolPage() }
        row.addView(more, keyParams(1.5f))
        for (s in page[2]) {
            val tv = makeKey(s, colorKey, colorKeyPressed)
            tv.setOnClickListener { listener?.onText(s) }
            row.addView(tv, keyParams(1f))
        }
        addBackspace(row, 1.5f)
        keysContainer.addView(row)
    }

    private fun buildSymbolKeyRow(symbols: List<String>): View {
        val row = rowContainer(charRowH)
        for (s in symbols) {
            val tv = makeKey(s, colorKey, colorKeyPressed)
            tv.setOnClickListener { listener?.onText(s) }
            row.addView(tv, keyParams(1f))
        }
        return row
    }

    // ── Shift 처리 ──────────────────────────────────────────────────

    private fun onShiftKey() {
        if (mode == Mode.KOREAN) {
            setKoreanShifted(!shifted)
            return
        }
        // 영어: 탭=대문자 1회, 빠른 더블탭=Caps Lock, 켜져 있으면 해제
        val now = System.currentTimeMillis()
        when {
            capsLock -> { capsLock = false; shifted = false }
            shifted && now - lastShiftTapTime < DOUBLE_TAP_MS -> { capsLock = true; shifted = false }
            shifted -> shifted = false
            else -> shifted = true
        }
        lastShiftTapTime = now
        refreshLetterCase()
        applyShiftAppearance()
    }

    private fun setKoreanShifted(on: Boolean) {
        shifted = on
        for ((tv, k) in charKeys) {
            tv.text = if (on && k.shifted != null) k.shifted else k.normal
        }
        applyShiftAppearance()
    }

    /** Shift/Caps 상태를 Shift 키 겉모습에 반영합니다. */
    private fun applyShiftAppearance() {
        val sk = shiftKey ?: return
        when {
            capsLock -> {
                sk.text = "⇪"
                sk.setTextColor(Color.WHITE)
                sk.background = keyBackground(colorAccent, colorAccentPressed)
            }
            shifted -> {
                sk.text = "⇧"
                sk.setTextColor(Color.WHITE)
                sk.background = keyBackground(colorAccent, colorAccentPressed)
            }
            else -> {
                sk.text = "⇧"
                sk.setTextColor(colorText)
                sk.background = keyBackground(colorSpecial, colorSpecialPressed)
            }
        }
    }

    /** Service가 입력창을 바꿀 때 호출: Shift/Caps 해제. */
    fun clearShift() {
        shifted = false
        capsLock = false
        if (mode == Mode.KOREAN) setKoreanShifted(false)
        else {
            refreshLetterCase()
            applyShiftAppearance()
        }
    }

    /** 예전 호환용: Shift를 끄는 데만 씁니다. */
    fun setShifted(on: Boolean) {
        if (!on) clearShift()
    }

    /**
     * '꾹 누르기' 반복 입력을 붙입니다. (백스페이스용)
     *  - 누르는 순간 1번 실행 → 약 0.4초 뒤부터 반복 → 반복할수록 빨라짐 → 떼면 즉시 멈춤
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachRepeatingTouch(view: View, action: () -> Unit) {
        var repeatCount = 0
        lateinit var repeater: Runnable
        repeater = Runnable {
            action()
            repeatCount++
            val interval = (FIRST_INTERVAL_MS - repeatCount * ACCEL_STEP_MS)
                .coerceAtLeast(MIN_INTERVAL_MS)
            repeatHandler.postDelayed(repeater, interval.toLong())
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    feedback(v)
                    repeatCount = 0
                    action()
                    repeatHandler.postDelayed(repeater, HOLD_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    repeatHandler.removeCallbacks(repeater)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        repeatHandler.removeCallbacksAndMessages(null)
    }

    // ── 화면 부품 만들기 도우미들 ────────────────────────────────────

    private fun rowContainer(heightDp: Float): LinearLayout {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, dp(heightDp))
        lp.topMargin = dp(3f)
        row.layoutParams = lp
        return row
    }

    private fun keyParams(weight: Float): LayoutParams {
        val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
        lp.leftMargin = dp(2f)
        lp.rightMargin = dp(2f)
        return lp
    }

    private fun spacer(weight: Float): View {
        val v = View(context)
        v.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
        return v
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeKey(label: String, normalColor: Int, pressedColor: Int): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.setTextColor(colorText)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        tv.isClickable = true
        tv.isFocusable = true
        tv.background = keyBackground(normalColor, pressedColor)
        // 모든 키에 공통 피드백: 누르는 순간(ACTION_DOWN) 소리/진동. false를 돌려주어
        // 원래의 클릭 처리는 그대로 진행되게 합니다. (백스페이스는 자체 터치리스너로 대체됨)
        tv.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) feedback(v)
            false
        }
        return tv
    }

    /** 키를 누를 때의 소리/진동 피드백. (설정에서 켠 것만 실행) */
    private fun feedback(v: View) {
        if (soundOn) audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        if (vibrateOn) v.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun keyBackground(normalColor: Int, pressedColor: Int): StateListDrawable {
        fun round(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(7f).toFloat()
            setColor(color)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), round(pressedColor))
            addState(intArrayOf(), round(normalColor))
        }
    }

    companion object {
        private const val HOLD_DELAY_MS = 400L
        private const val FIRST_INTERVAL_MS = 120
        private const val ACCEL_STEP_MS = 8
        private const val MIN_INTERVAL_MS = 35
        private const val DOUBLE_TAP_MS = 320L
    }
}
