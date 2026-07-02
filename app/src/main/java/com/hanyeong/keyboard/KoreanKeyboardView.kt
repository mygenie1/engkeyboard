package com.hanyeong.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
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
        setPadding(dp(3f), dp(6f), dp(3f), dp(8f))

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
        val row = rowContainer(44f)
        for (n in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")) {
            val tv = makeKey(n, colorKey, colorKeyPressed)
            tv.setOnClickListener { listener?.onText(n) }
            row.addView(tv, keyParams(1f))
        }
        return row
    }

    /** 자모 키들로만 이루어진 한 줄을 만듭니다. */
    private fun buildCharRow(keys: List<CharKey>, sideSpacerWeight: Float = 0f): View {
        val row = rowContainer()
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
        val row = rowContainer()

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

        val back = makeKey("⌫", colorSpecial, colorSpecialPressed)
        back.setOnClickListener { listener?.onBackspace() }
        row.addView(back, keyParams(1.5f))
        return row
    }

    private fun buildRow4(): View {
        val row = rowContainer()

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
}
