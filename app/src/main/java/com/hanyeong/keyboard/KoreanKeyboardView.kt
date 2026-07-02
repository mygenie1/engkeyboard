package com.hanyeong.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.hanyeong.keyboard.dict.DictEntry

/**
 * 두벌식 한글 자판 + 추천 바 + 학습 카드를 '직접 그리는' 화면입니다.
 *
 * 세로 구조:
 *   [추천 바]  ← 항상 같은 높이로 자리를 차지 (비어 있어도) → 나타나도 화면이 안 밀림
 *   [본체]     ← 자판 + 그 위에 겹쳐지는 학습 카드
 *
 * 학습 카드는 자판 위에 '겹쳐서' 열립니다. 전체 높이가 변하지 않으므로
 * 카드가 열리고 닫혀도 입력창이 위아래로 출렁이지 않습니다.
 */
@SuppressLint("ViewConstructor")
class KoreanKeyboardView(context: Context) : LinearLayout(context) {

    /** 키를 눌렀을 때 키보드 본체(Service)에 알려 주는 통로입니다. */
    interface Listener {
        fun onJamo(jamo: Char)   // 자모 키(ㄱ, ㅏ ...)
        fun onBackspace()        // 지우기
        fun onSpace()            // 띄어쓰기
        fun onEnter()            // 줄바꿈/전송
        fun onText(text: String) // 쉼표, 마침표, 숫자 등 일반 글자
    }

    var listener: Listener? = null

    private var shifted = false
    private val charKeys = mutableListOf<Pair<TextView, CharKey>>()
    private data class CharKey(val normal: String, val shifted: String? = null)

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val isLandscape =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 키/바 높이: 가로 모드에서는 더 낮게
    private val charRowH = if (isLandscape) 34f else 52f
    private val numRowH = if (isLandscape) 28f else 44f
    private val suggestionBarH = if (isLandscape) 34f else 46f

    private val repeatHandler = Handler(Looper.getMainLooper())

    // 색상
    private val colorBoard = Color.parseColor("#D5D8DE")
    private val colorKey = Color.parseColor("#FFFFFF")
    private val colorKeyPressed = Color.parseColor("#C7CBD1")
    private val colorSpecial = Color.parseColor("#B9BEC7")
    private val colorSpecialPressed = Color.parseColor("#9AA0AB")
    private val colorText = Color.parseColor("#1C1E21")
    private val colorChip = Color.parseColor("#FFFFFF")
    private val colorChipText = Color.parseColor("#1A5FB4")
    private val colorCardBg = Color.parseColor("#FFFFFF")
    private val colorMeaning = Color.parseColor("#374151")
    private val colorSub = Color.parseColor("#6B7280")

    // 추천 바와 학습 카드의 부품들
    private lateinit var suggestionBar: LinearLayout
    private lateinit var cardView: LinearLayout
    private lateinit var cardEnglish: TextView
    private lateinit var cardPron: TextView
    private lateinit var cardMeaning: TextView
    private lateinit var cardExample: TextView

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

        val keys = LinearLayout(context)
        keys.orientation = VERTICAL
        val vPad = if (isLandscape) 3f else 6f
        keys.setPadding(dp(3f), dp(vPad), dp(3f), dp(vPad + 2f))
        keys.addView(buildNumberRow())
        keys.addView(buildCharRow(listOf(
            CharKey("ㅂ", "ㅃ"), CharKey("ㅈ", "ㅉ"), CharKey("ㄷ", "ㄸ"),
            CharKey("ㄱ", "ㄲ"), CharKey("ㅅ", "ㅆ"),
            CharKey("ㅛ"), CharKey("ㅕ"), CharKey("ㅑ"),
            CharKey("ㅐ", "ㅒ"), CharKey("ㅔ", "ㅖ")
        )))
        keys.addView(buildCharRow(
            listOf(
                CharKey("ㅁ"), CharKey("ㄴ"), CharKey("ㅇ"), CharKey("ㄹ"), CharKey("ㅎ"),
                CharKey("ㅗ"), CharKey("ㅓ"), CharKey("ㅏ"), CharKey("ㅣ")
            ),
            sideSpacerWeight = 0.5f
        ))
        keys.addView(buildRow3())
        keys.addView(buildRow4())
        body.addView(
            keys,
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
     * Service가 '단어를 다 쳤을 때' 호출합니다.
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
        cardPron.text = e.pronunciation
        cardMeaning.text = "뜻 · ${e.meaning}"
        cardExample.text = "예문 · ${e.example}"
        cardView.visibility = VISIBLE
    }

    /** 학습 카드를 닫습니다. (다시 타이핑을 시작하면 호출됨) */
    fun hideCard() {
        if (cardView.visibility != GONE) cardView.visibility = GONE
    }

    // ── 자판 줄 만들기 ──────────────────────────────────────────────

    private fun buildNumberRow(): View {
        val row = rowContainer(numRowH)
        for (n in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) {
            val tv = makeKey(n, colorKey, colorKeyPressed)
            tv.setOnClickListener { listener?.onText(n) }
            row.addView(tv, keyParams(1f))
        }
        return row
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

    private fun buildRow3(): View {
        val row = rowContainer(charRowH)

        val shift = makeKey("⇧", colorSpecial, colorSpecialPressed)
        shift.setOnClickListener { toggleShift() }
        row.addView(shift, keyParams(1.5f))

        for (s in listOf("ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ")) {
            val k = CharKey(s)
            val tv = makeKey(s, colorKey, colorKeyPressed)
            tv.setOnClickListener { onCharKeyTapped(k) }
            charKeys.add(tv to k)
            row.addView(tv, keyParams(1f))
        }

        // 지우기: 짧게 누르면 1글자, 꾹 누르면 점점 빨라지는 연속 삭제.
        val back = makeKey("⌫", colorSpecial, colorSpecialPressed)
        attachRepeatingTouch(back) { listener?.onBackspace() }
        row.addView(back, keyParams(1.5f))
        return row
    }

    private fun buildRow4(): View {
        val row = rowContainer(charRowH)

        val comma = makeKey(",", colorSpecial, colorSpecialPressed)
        comma.setOnClickListener { listener?.onText(",") }
        row.addView(comma, keyParams(1f))

        val space = makeKey("", colorKey, colorKeyPressed)
        space.setOnClickListener { listener?.onSpace() }
        row.addView(space, keyParams(6f))

        val period = makeKey(".", colorSpecial, colorSpecialPressed)
        period.setOnClickListener { listener?.onText(".") }
        row.addView(period, keyParams(1f))

        val enter = makeKey("⏎", colorSpecial, colorSpecialPressed)
        enter.setOnClickListener { listener?.onEnter() }
        row.addView(enter, keyParams(2f))
        return row
    }

    private fun onCharKeyTapped(k: CharKey) {
        val text = if (shifted && k.shifted != null) k.shifted else k.normal
        listener?.onJamo(text[0])
        if (shifted) setShifted(false) // 한 글자 치면 Shift 자동 해제
    }

    private fun toggleShift() = setShifted(!shifted)

    fun setShifted(on: Boolean) {
        shifted = on
        for ((tv, k) in charKeys) {
            tv.text = if (on && k.shifted != null) k.shifted else k.normal
        }
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

    private fun makeKey(label: String, normalColor: Int, pressedColor: Int): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.setTextColor(colorText)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        tv.isClickable = true
        tv.isFocusable = true
        tv.background = keyBackground(normalColor, pressedColor)
        return tv
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
    }
}
