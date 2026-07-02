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
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 두벌식 한글 자판을 '직접 그리는' 화면입니다.
 *
 * 안드로이드의 오래된 기본 키보드 틀을 쓰지 않고,
 * 줄(row)과 키(TextView)를 코드로 하나하나 배치합니다.
 * 그래서 키 높이·색·모양을 자유롭게 바꿀 수 있고,
 * 나중에 이 위에 '추천 바'와 '학습 카드'를 얹기 쉽습니다.
 */
@SuppressLint("ViewConstructor")
class KoreanKeyboardView(context: Context) : LinearLayout(context) {

    /** 키를 눌렀을 때 키보드 본체(Service)에 알려 주는 통로입니다. */
    interface Listener {
        fun onJamo(jamo: Char)   // 자모 키(ㄱ, ㅏ ...)
        fun onBackspace()        // 지우기
        fun onSpace()            // 띄어쓰기
        fun onEnter()            // 줄바꿈/전송
        fun onText(text: String) // 쉼표, 마침표 등 일반 글자
    }

    var listener: Listener? = null

    // 지금 Shift(쌍자음)가 눌린 상태인지
    private var shifted = false

    // 자모 키들을 기억해 두었다가 Shift 상태에 따라 글자를 바꿔 답니다.
    private val charKeys = mutableListOf<Pair<TextView, CharKey>>()

    /** 자모 키 하나의 정보. normal=평소 글자, shifted=Shift 눌렀을 때 글자(없으면 동일). */
    private data class CharKey(val normal: String, val shifted: String? = null)

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()
    private fun sp(v: Float): Float = v

    // 지금 화면이 가로 모드인지 확인합니다.
    private val isLandscape =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 키 높이: 가로 모드에서는 화면이 낮으므로 훨씬 작게 잡습니다.
    private val charRowH = if (isLandscape) 34f else 52f   // 자모 줄 높이(dp)
    private val numRowH = if (isLandscape) 28f else 44f    // 숫자 줄 높이(dp)

    // 백스페이스 '꾹 누르기'용 타이머. 화면이 사라질 때 확실히 멈추도록 하나만 둡니다.
    private val repeatHandler = Handler(Looper.getMainLooper())

    // 색상 (은은한 회색 배경 + 흰 글쇠)
    private val colorBoard = Color.parseColor("#D5D8DE")
    private val colorKey = Color.parseColor("#FFFFFF")
    private val colorKeyPressed = Color.parseColor("#C7CBD1")
    private val colorSpecial = Color.parseColor("#B9BEC7")
    private val colorSpecialPressed = Color.parseColor("#9AA0AB")
    private val colorText = Color.parseColor("#1C1E21")

    init {
        orientation = VERTICAL
        setBackgroundColor(colorBoard)
        // 가로 모드에서는 위아래 여백도 줄여 전체 높이를 더 낮춥니다.
        val vPad = if (isLandscape) 3f else 6f
        setPadding(dp(3f), dp(vPad), dp(3f), dp(vPad + 2f))

        // 맨 윗줄: 숫자 1234567890 (항상 표시)
        addView(buildNumberRow())

        // 1번째 줄: ㅂㅈㄷㄱㅅ ㅛㅕㅑㅐㅔ
        addView(buildCharRow(listOf(
            CharKey("ㅂ", "ㅃ"), CharKey("ㅈ", "ㅉ"), CharKey("ㄷ", "ㄸ"),
            CharKey("ㄱ", "ㄲ"), CharKey("ㅅ", "ㅆ"),
            CharKey("ㅛ"), CharKey("ㅕ"), CharKey("ㅑ"),
            CharKey("ㅐ", "ㅒ"), CharKey("ㅔ", "ㅖ")
        )))

        // 2번째 줄: ㅁㄴㅇㄹㅎ ㅗㅓㅏㅣ (9개라 좌우로 살짝 안쪽에 배치)
        addView(buildCharRow(
            listOf(
                CharKey("ㅁ"), CharKey("ㄴ"), CharKey("ㅇ"), CharKey("ㄹ"), CharKey("ㅎ"),
                CharKey("ㅗ"), CharKey("ㅓ"), CharKey("ㅏ"), CharKey("ㅣ")
            ),
            sideSpacerWeight = 0.5f
        ))

        // 3번째 줄: [Shift] ㅋㅌㅊㅍ ㅠㅜㅡ [지우기]
        addView(buildRow3())

        // 4번째 줄: [쉼표] [스페이스] [마침표] [엔터]
        addView(buildRow4())
    }

    /** 숫자 줄(1~0)을 만듭니다. 숫자는 조합 대상이 아니라 일반 글자로 바로 입력됩니다. */
    private fun buildNumberRow(): View {
        val row = rowContainer(numRowH)
        for (n in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) {
            val tv = makeKey(n, colorKey, colorKeyPressed)
            tv.setOnClickListener { listener?.onText(n) }
            row.addView(tv, keyParams(1f))
        }
        return row
    }

    /** 자모 키들로만 이루어진 한 줄을 만듭니다. */
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
        if (shifted) setShifted(false) // 한 글자 치면 Shift 자동 해제 (한 번만 적용)
    }

    private fun toggleShift() = setShifted(!shifted)

    /** Shift 상태를 바꾸고, 자모 키들의 글자를 그에 맞게 다시 답니다. */
    fun setShifted(on: Boolean) {
        shifted = on
        for ((tv, k) in charKeys) {
            tv.text = if (on && k.shifted != null) k.shifted else k.normal
        }
    }

    /**
     * '꾹 누르기' 반복 입력을 붙입니다. (백스페이스용)
     *  - 누르는 순간 1번 실행
     *  - 약 0.4초 뒤부터 반복 시작
     *  - 반복할수록 간격이 짧아져 점점 빨라짐
     *  - 손을 떼거나 손가락이 키 밖으로 나가면 즉시 멈춤
     * 삼성 키보드/Gboard의 표준 동작과 같습니다.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachRepeatingTouch(view: View, action: () -> Unit) {
        var repeatCount = 0
        lateinit var repeater: Runnable
        repeater = Runnable {
            action()
            repeatCount++
            // 반복 횟수가 늘수록 간격을 줄여 가속시키되, 최소 간격 아래로는 안 내려갑니다.
            val interval = (FIRST_INTERVAL_MS - repeatCount * ACCEL_STEP_MS)
                .coerceAtLeast(MIN_INTERVAL_MS)
            repeatHandler.postDelayed(repeater, interval.toLong())
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    repeatCount = 0
                    action()                                   // 즉시 1번 삭제
                    repeatHandler.postDelayed(repeater, HOLD_DELAY_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    repeatHandler.removeCallbacks(repeater)     // 즉시 멈춤
                    true
                }
                else -> false
            }
        }
    }

    // 화면(자판)이 사라질 때, 혹시 돌고 있던 반복 삭제를 확실히 멈춥니다.
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        repeatHandler.removeCallbacksAndMessages(null)
    }

    // ── 화면 부품 만들기 도우미들 ────────────────────────────────────

    private fun rowContainer(heightDp: Float = 52f): LinearLayout {
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
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(20f))
        tv.isClickable = true
        tv.isFocusable = true
        tv.background = keyBackground(normalColor, pressedColor)
        return tv
    }

    /** 평소/눌렀을 때 두 가지 모양을 가진 둥근 사각형 키 배경을 만듭니다. */
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
        // 백스페이스 '꾹 누르기' 반복 타이밍(밀리초)
        private const val HOLD_DELAY_MS = 400L     // 누른 뒤 반복이 시작되기까지의 시간
        private const val FIRST_INTERVAL_MS = 120  // 반복 시작 직후의 간격
        private const val ACCEL_STEP_MS = 8        // 반복할 때마다 줄어드는 간격(가속)
        private const val MIN_INTERVAL_MS = 35     // 가장 빠를 때의 간격(하한)
    }
}
