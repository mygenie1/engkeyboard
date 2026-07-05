package com.hanyeong.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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
import android.widget.HorizontalScrollView
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
        fun onLetter(text: String) // 영어 글자 키(a~z). 영어 '단어'로 추적됨
        fun onText(text: String) // 숫자/기호 등 단어 경계로 취급되는 글자
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

    // 키/바 높이(dp): 설정의 '키보드 높이'(낮음40/보통46/높음52). 가로 모드는 30.
    private val charRowH: Float = when {
        isLandscape -> 30f
        Settings.height(context) == Settings.HEIGHT_LOW -> 40f
        Settings.height(context) == Settings.HEIGHT_HIGH -> 52f
        else -> 46f
    }
    private val numRowH: Float = if (isLandscape) 26f else charRowH - 6f
    private val suggestionBarH = if (isLandscape) 34f else 40f   // 추천 바 고정 높이

    // 키 입력 피드백(소리/진동) 설정값을 만들 때 한 번 읽어 둡니다.
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val soundOn = Settings.soundEnabled(context)
    private val vibrateOn = Settings.vibrationEnabled(context)

    private val repeatHandler = Handler(Looper.getMainLooper())

    private val mediumTypeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)

    // ── 색상 토큰: "Quiet Slate"(톤온톤 청회) 라이트/다크 2벌 (시스템 다크 모드를 따름) ──
    private val isDark =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    private fun themed(light: String, dark: String): Int =
        Color.parseColor(if (isDark) dark else light)

    private val colorBoard = themed("#E9EDF2", "#14181D")          // bg (키보드 배경)
    private val colorKey = themed("#FFFFFF", "#272E36")            // key (일반 키)
    private val colorKeyPressed = themed("#D3DAE4", "#333C46")     // pressed (눌림)
    private val colorText = themed("#202B36", "#E7EBF0")           // keyText
    private val colorSpecial = themed("#DBE1E9", "#1D232A")        // fnBg (기능 키)
    private val colorSpecialText = themed("#3E4C59", "#9AA6B4")    // fnText (기능 키 글자)
    private val colorSpecialPressed = colorKeyPressed             // 기능 키 눌림도 공통 pressed
    private val colorAccent = themed("#46698C", "#85A9CD")         // accent
    private val colorAccentPressed = themed("#3A5578", "#6E8CAF")  // 활성 Shift 눌림(파생색)
    private val colorChip = themed("#FFFFFF", "#272E36")           // chipBg
    private val colorChipText = themed("#3E5F80", "#85A9CD")       // chipText
    private val colorChipBorder = themed("#C7D2DF", "#3A434E")     // chipBorder (1dp)
    private val colorCardBg = themed("#FFFFFF", "#1D232A")         // cardBg
    private val colorSub = themed("#6B7889", "#8D99A8")            // sub (보조 텍스트)
    private val colorDivider = themed("#E3E8EF", "#333C46")        // divider (카드 구분선)
    private val colorMeaning = colorText                          // 품사·뜻 = keyText
    private val colorKeyShadow = themed("#29000000", "#66000000")  // 키 하단 1dp 엣지(검정 16%/40%)

    // 추천 바와 학습 카드의 부품들
    private lateinit var suggestionScroll: HorizontalScrollView
    private lateinit var suggestionBar: LinearLayout
    private lateinit var keysContainer: LinearLayout   // 자판 본체(모드마다 다시 그림)
    private lateinit var cardView: FrameLayout         // 카드 스크림(바깥 탭 가로채기)
    private lateinit var cardScroll: android.widget.ScrollView  // 카드 내용 스크롤(높이 제한)
    private lateinit var cardContext: TextView         // 좌상단 컨텍스트 라벨
    private lateinit var cardEnglish: TextView         // 표제어
    private lateinit var cardPron: TextView            // 발음기호
    private lateinit var cardMeaning: TextView         // 품사·뜻 (역방향엔 영어·발음)
    private lateinit var cardDivider: View             // 예문 위 구분선
    private lateinit var cardExample: TextView         // 예문

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
        //    가로 스크롤 안에 담아, 추천이 바 폭을 넘으면 좌우로 밀어 볼 수 있게 합니다.
        //    넘칠 때는 오른쪽 가장자리가 흐려지며 잘려, "더 있음"을 넌지시 알립니다.
        suggestionBar = buildSuggestionBar()
        suggestionScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false      // 스크롤바 막대는 숨김(깔끔하게)
            isHorizontalFadingEdgeEnabled = true       // 넘칠 때 가장자리 페이드로 암시
            setFadingEdgeLength(dp(30f))
            overScrollMode = View.OVER_SCROLL_NEVER    // 끝에서 당길 때 파란 광택 없음
            // 스크롤 내용은 폭을 넘을 수 있어야 하므로 가로는 WRAP_CONTENT.
            addView(
                suggestionBar,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        addView(
            suggestionScroll,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(suggestionBarH))
        )

        // 2) 본체: 자판 + 카드(겹침)
        val body = FrameLayout(context)
        body.clipChildren = false       // 카드 그림자가 잘리지 않도록
        body.clipToPadding = false

        keysContainer = LinearLayout(context)
        keysContainer.orientation = VERTICAL
        val vPad = if (isLandscape) 3f else 6f
        keysContainer.setPadding(dp(4f), dp(vPad), dp(4f), dp(vPad + 2f))   // 키보드 좌우 패딩 4dp
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
        bar.setPadding(dp(8f), 0, dp(8f), 0)   // 좌측 시작 여백 8dp
        return bar
    }

    /**
     * 추천 단어들을 추천 바에 표시합니다. (빈 목록이면 바를 비웁니다)
     *
     * @param reverse 역방향(영→한) 추천이면 true. 칩에 '한글 단어'(+짧은 뜻)를
     *                보여 줍니다. (기본 한→영 추천은 false: 칩에 영어 단어)
     */
    fun showSuggestions(entries: List<DictEntry>, reverse: Boolean = false) {
        suggestionBar.removeAllViews()
        for (e in entries) {
            val secondary = if (reverse) shortGloss(e) else null
            val chip = makeChip(if (reverse) e.korean else e.english, secondary)
            chip.setOnClickListener { openCard(e, reverse) }
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, dp(if (isLandscape) 26f else 30f))
            lp.marginEnd = dp(7f)   // 칩 간격 7dp
            suggestionBar.addView(chip, lp)
        }
        // 새 추천이 오면 항상 맨 왼쪽(첫 단어)부터 보이도록 스크롤을 되돌립니다.
        if (::suggestionScroll.isInitialized) suggestionScroll.scrollTo(0, 0)
    }

    /** 역방향 칩의 보조 뜻: 표제어와 다르고 짧을 때만 (예: "명사"). */
    private fun shortGloss(e: DictEntry): String? {
        val m = e.meaning.trim()
        return if (m.isNotEmpty() && m != e.korean && m.length <= 10) m else null
    }

    /** 추천 칩: 완전 라운드(반경 15) + 1dp 테두리. 필요 시 보조 뜻을 옆에 붙입니다. */
    private fun makeChip(primary: String, secondary: String?): View {
        val chip = LinearLayout(context)
        chip.orientation = HORIZONTAL
        chip.gravity = Gravity.CENTER_VERTICAL
        chip.setPadding(dp(12f), 0, dp(12f), 0)   // 좌우 패딩 12dp
        chip.isClickable = true
        chip.background = chipBackground()

        val main = TextView(context)
        main.text = primary
        main.setTextColor(colorChipText)
        main.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        main.typeface = mediumTypeface
        chip.addView(main)

        if (secondary != null) {
            val sub = TextView(context)
            sub.text = secondary
            sub.setTextColor(colorSub)
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(6f)   // 단어와 6dp 간격
            chip.addView(sub, lp)
        }
        return chip
    }

    private fun chipBackground(): StateListDrawable {
        fun bg(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(15f).toFloat()
            setColor(color)
            setStroke(dp(1f), colorChipBorder)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), bg(colorKeyPressed))
            addState(intArrayOf(), bg(colorChip))
        }
    }

    // ── 학습 카드 ───────────────────────────────────────────────────

    private fun buildCardView(): FrameLayout {
        // 스크림: 자판 위 전체를 덮어 '카드 밖 탭'을 가로채 카드를 닫습니다.
        // (키 입력이 새어 나가지 않도록 전면을 덮되, 배경은 투명)
        val scrim = FrameLayout(context)
        scrim.isClickable = true
        scrim.setOnClickListener { hideCard() }
        scrim.clipChildren = false
        scrim.clipToPadding = false

        // 패널: 둥근 모서리(10dp) + 그림자(elevation)로 떠 있는 카드.
        val panel = LinearLayout(context)
        panel.orientation = VERTICAL
        panel.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10f).toFloat()
            setColor(colorCardBg)
        }
        panel.elevation = dp(6f).toFloat()   // 그림자 y4 blur16 근사

        // 내용: 세로 스크롤 안에 담아, 길어도 카드가 자판 높이를 넘지 않게 합니다.
        val content = LinearLayout(context)
        content.orientation = VERTICAL
        content.setPadding(dp(16f), dp(12f), dp(16f), dp(12f))

        // 윗줄: 컨텍스트 라벨(좌) + 닫기 ✕(우, 터치 영역 넉넉히)
        val topbar = LinearLayout(context)
        topbar.orientation = HORIZONTAL
        topbar.gravity = Gravity.CENTER_VERTICAL
        cardContext = TextView(context)
        cardContext.setTextColor(colorSub)
        cardContext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        topbar.addView(cardContext, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        val close = TextView(context)
        close.text = "✕"
        close.setTextColor(colorSub)
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        close.setPadding(dp(14f), dp(8f), dp(6f), dp(8f))
        close.isClickable = true
        close.setOnClickListener { hideCard() }
        topbar.addView(close)
        content.addView(topbar)

        // 표제어 + 발음기호 (아래 정렬로 baseline 근사)
        val headline = LinearLayout(context)
        headline.orientation = HORIZONTAL
        headline.gravity = Gravity.BOTTOM
        cardEnglish = TextView(context)
        cardEnglish.setTextColor(colorText)
        cardEnglish.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        cardEnglish.setTypeface(cardEnglish.typeface, android.graphics.Typeface.BOLD)
        headline.addView(cardEnglish)
        cardPron = TextView(context)
        cardPron.setTextColor(colorAccent)
        cardPron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        val pronLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        pronLp.marginStart = dp(10f)
        pronLp.bottomMargin = dp(2f)
        headline.addView(cardPron, pronLp)
        content.addView(headline)

        // 품사·뜻 (역방향에선 영어 단어·발음)
        cardMeaning = TextView(context)
        cardMeaning.setTextColor(colorMeaning)
        cardMeaning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        val meaningLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        meaningLp.topMargin = dp(6f)
        content.addView(cardMeaning, meaningLp)

        // 구분선 (예문이 없으면 숨김)
        cardDivider = View(context)
        cardDivider.setBackgroundColor(colorDivider)
        val divLp = LayoutParams(LayoutParams.MATCH_PARENT, dp(1f))
        divLp.topMargin = dp(10f)
        divLp.bottomMargin = dp(10f)
        content.addView(cardDivider, divLp)

        // 예문
        cardExample = TextView(context)
        cardExample.setTextColor(colorText)
        cardExample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        cardExample.setLineSpacing(0f, 1.5f)   // 줄간 1.5
        content.addView(cardExample, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // 광고 자리(현재 미사용, 기본 높이 0). 켜지면 내용 하단에 50dp 배너 자리.
        content.addView(buildAdSlot(), adSlotParams())

        cardScroll = BoundedCardScroll(context)
        cardScroll.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        panel.addView(cardScroll, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // 패널을 스크림 위에 6dp 여백 + 상단 정렬로 얹습니다.
        val panelLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        panelLp.setMargins(dp(6f), dp(6f), dp(6f), dp(6f))
        panelLp.gravity = Gravity.TOP
        scrim.addView(panel, panelLp)

        return scrim
    }

    /**
     * 학습 카드 내용용 세로 스크롤. **높이를 절대 '키보드 본체(keysContainer)
     * 높이' 이내로 강제**합니다. 내용이 더 길면 카드가 커지는 대신 내부에서
     * 스크롤됩니다. → 카드가 입력창·앱 화면을 가리는 일을 원천 차단.
     * (마일스톤 4의 "키보드 높이 불변, 오버레이" 원칙을 지키는 안전장치)
     */
    private inner class BoundedCardScroll(context: Context) : android.widget.ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val keyboardH = keysContainer.height
            // 카드 패널의 상·하 여백(6+6dp) 몫을 빼, 패널까지 포함해도 자판 높이 이내로.
            val avail = keyboardH - dp(12f)
            val bounded = if (keyboardH > 0) {
                val incoming = if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.UNSPECIFIED)
                    Int.MAX_VALUE else View.MeasureSpec.getSize(heightMeasureSpec)
                View.MeasureSpec.makeMeasureSpec(
                    CardBounds.clamp(incoming, avail), View.MeasureSpec.AT_MOST
                )
            } else {
                heightMeasureSpec
            }
            super.onMeasure(widthMeasureSpec, bounded)
        }
    }

    /**
     * 학습 카드 하단 광고 자리. 지금은 '빈 자리'일 뿐이라 아무 네트워크·SDK도
     * 쓰지 않습니다. 켜졌을 때만 옅은 배경과 '광고 자리' 안내를 보여 줍니다.
     */
    private fun buildAdSlot(): View {
        val slot = FrameLayout(context)
        slot.visibility = if (AD_PLACEHOLDER_ENABLED) VISIBLE else GONE
        if (AD_PLACEHOLDER_ENABLED) {
            slot.setBackgroundColor(Color.parseColor("#EDEFF2"))
            val label = TextView(context)
            label.text = "광고 자리"
            label.gravity = Gravity.CENTER
            label.setTextColor(colorSub)
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            slot.addView(
                label,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        return slot
    }

    /** 광고 자리 크기: 켜지면 표준 배너(폭 전체 × 50dp), 꺼지면 높이 0. */
    private fun adSlotParams(): LayoutParams {
        val h = if (AD_PLACEHOLDER_ENABLED) dp(50f) else 0
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, h)
        if (AD_PLACEHOLDER_ENABLED) lp.topMargin = dp(10f)
        return lp
    }

    /**
     * 학습 카드를 엽니다.
     *  - 한→영(reverse=false): 영어를 표제어 + 발음기호, 아래에 한글 뜻.
     *  - 영→한(reverse=true) : 한글을 표제어, 아래에 영어 단어·발음.
     * 2군(간이) 항목은 발음·예문 줄을 뷰째 숨겨(GONE) 빈 줄 없이 축소합니다.
     */
    private fun openCard(e: DictEntry, reverse: Boolean = false) {
        val input = if (reverse) e.english else e.korean
        cardContext.text = (if (reverse) "영→한" else "한→영") + " · 방금 입력: " + input

        if (reverse) {
            cardEnglish.text = e.korean
            setOptionalLine(cardPron, "", "")   // 표제어(한글) 옆 발음 자리 비움
            val eng = e.english + if (e.pronunciation.isNotBlank()) "  " + e.pronunciation else ""
            setOptionalLine(cardMeaning, eng, "")
        } else {
            cardEnglish.text = e.english
            setOptionalLine(cardPron, e.pronunciation, "")
            setOptionalLine(cardMeaning, e.meaning, "")
        }
        setOptionalLine(cardExample, e.example, "")
        cardDivider.visibility = if (e.example.isBlank()) GONE else VISIBLE

        cardView.visibility = VISIBLE
        cardScroll.scrollTo(0, 0)   // 항상 맨 위(표제어)부터 보이도록
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
            val tv = makeKey(n, colorKey, colorKeyPressed, sizeSp = 16f)   // 숫자줄 16sp
            tv.setOnClickListener { listener?.onText(n) }
            row.addView(tv, keyParams(1f))
        }
        return row
    }

    /**
     * 맨 아랫줄. 모드에 따라 왼쪽 키가 달라집니다.
     *   글자판: [?123] [한/영] [,] [스페이스] [.] [⏎]
     *   기호판: [ABC·가] [,] [스페이스] [.] [⏎]
     * 쉼표(,)는 모든 모드에서 스페이스 바로 왼쪽에 오도록 통일했습니다.
     */
    private fun buildBottomRow(): View {
        val row = rowContainer(charRowH)

        if (mode == Mode.SYMBOL) {
            val back = makeFnKey(if (lastLetterMode == Mode.KOREAN) "가" else "ABC", 12f)
            back.setOnClickListener { toggleSymbols() }
            row.addView(back, keyParams(2f))

            addComma(row, 1f)
            addSpace(row, 5f)
            addPeriod(row, 1f)
            addEnter(row, 2f)
        } else {
            val sym = makeFnKey("?123", 12f)
            sym.setOnClickListener { toggleSymbols() }
            row.addView(sym, keyParams(1.5f))

            val lang = makeFnKey("한/영", 12f)
            lang.setOnClickListener { toggleLanguage() }
            row.addView(lang, keyParams(1.5f))

            addComma(row, 1f)

            addSpace(row, 4f)
            addPeriod(row, 1f)
            addEnter(row, 2f)
        }
        return row
    }

    // 쉼표·마침표는 기능 키 목록에 없으므로 일반 키(흰 배경)로 둡니다.
    private fun addComma(row: LinearLayout, weight: Float) {
        val comma = makeKey(",", colorKey, colorKeyPressed)
        comma.setOnClickListener { listener?.onText(",") }
        row.addView(comma, keyParams(weight))
    }

    private fun addPeriod(row: LinearLayout, weight: Float) {
        val period = makeKey(".", colorKey, colorKeyPressed)
        period.setOnClickListener { listener?.onText(".") }
        row.addView(period, keyParams(weight))
    }

    private fun addSpace(row: LinearLayout, weight: Float) {
        val space = makeKey("", colorKey, colorKeyPressed)
        space.setOnClickListener { listener?.onSpace() }
        row.addView(space, keyParams(weight))
    }

    private fun addEnter(row: LinearLayout, weight: Float) {
        val enter = makeFnKey("⏎", 19f)
        enter.setOnClickListener { listener?.onEnter() }
        row.addView(enter, keyParams(weight))
    }

    /** 왼쪽 특수키(Shift/기호더보기) + 가운데 키들 + 오른쪽 백스페이스 형태의 줄. */
    private fun addBackspace(row: LinearLayout, weight: Float) {
        val back = makeFnKey("⌫", 19f)
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
        val shift = makeFnKey("⇧", 19f)
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
        val shift = makeFnKey("⇧", 19f)
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
        listener?.onLetter(if (upper) base.uppercase() else base)
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
        val more = makeFnKey(if (symbolPage == 0) "1/2" else "2/2", 12f)
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
                sk.setTextColor(colorSpecialText)
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
        lp.topMargin = dp(7f)   // 세로 키 간격 7dp
        row.layoutParams = lp
        return row
    }

    private fun keyParams(weight: Float): LayoutParams {
        val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
        lp.leftMargin = dp(2.5f)   // 가로 키 간격 5dp (양쪽 2.5)
        lp.rightMargin = dp(2.5f)
        return lp
    }

    private fun spacer(weight: Float): View {
        val v = View(context)
        v.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight)
        return v
    }

    /** 기능 키(⇧ ⌫ ?123 한/영 ⏎ 가/ABC 1/2 …): fnBg 배경 + fnText 글자. */
    private fun makeFnKey(label: String, sizeSp: Float): TextView =
        makeKey(label, colorSpecial, colorSpecialPressed, colorSpecialText, sizeSp, medium = true)

    @SuppressLint("ClickableViewAccessibility")
    private fun makeKey(
        label: String,
        normalColor: Int,
        pressedColor: Int,
        textColor: Int = colorText,
        sizeSp: Float = 18f,
        medium: Boolean = false,
    ): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.setTextColor(textColor)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        if (medium) tv.typeface = mediumTypeface
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

    /**
     * 키 배경. 모서리 반경 7dp에, 아래쪽에 은은한 1dp 그림자 엣지를 넣습니다.
     * (애니메이션 없이 눌리면 즉시 pressed 색으로 교체 — 입력 지연 0 원칙)
     */
    private fun keyBackground(normalColor: Int, pressedColor: Int): StateListDrawable {
        fun round(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(7f).toFloat()
            setColor(color)
        }
        // [아래=그림자 색] 위에 [키 색]을 1dp 위로 얹어, 바닥에 1dp 그림자만 노출.
        fun layered(color: Int): Drawable {
            val ld = LayerDrawable(arrayOf(round(colorKeyShadow), round(color)))
            ld.setLayerInset(1, 0, 0, 0, dp(1f))
            return ld
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), layered(pressedColor))
            addState(intArrayOf(), layered(normalColor))
        }
    }

    companion object {
        private const val HOLD_DELAY_MS = 400L
        private const val FIRST_INTERVAL_MS = 120
        private const val ACCEL_STEP_MS = 8
        private const val MIN_INTERVAL_MS = 35
        private const val DOUBLE_TAP_MS = 320L

        // 학습 카드 하단 '광고 자리'를 펼칠지 여부. 지금은 자리만 잡아 두고
        // 끄기(false) 상태로 둡니다. 실제 배너 광고를 붙일 때 true로 바꾸면
        // 됩니다. (true만으로는 광고가 뜨지 않습니다 — SDK·권한·심사가 별도 필요.)
        private const val AD_PLACEHOLDER_ENABLED = false
    }
}
