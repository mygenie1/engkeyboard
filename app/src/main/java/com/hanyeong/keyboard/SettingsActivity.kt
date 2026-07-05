package com.hanyeong.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/**
 * 앱을 누르면 열리는 설정 화면입니다. (키보드 설정에서도 이 화면으로 연결됩니다)
 *
 * 화면 구성:
 *   - 입력       : 키 입력음 / 진동 / 추천 바 표시 (켜고 끄기)
 *   - 모양       : 키보드 높이 (낮음 / 보통 / 높음)
 *   - 사전 출처   : 국립국어원 한국어기초사전 출처·라이선스 (CC BY-SA, 필수 표시)
 *   - 정보       : 앱 이름/버전
 *
 * 별도 화면 파일(XML) 없이 코드로 직접 그립니다. (키보드 화면과 같은 방식)
 */
class SettingsActivity : Activity() {

    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Float): Int = (v * density).toInt()

    private val colorBg = Color.parseColor("#F2F3F5")
    private val colorCard = Color.parseColor("#FFFFFF")
    private val colorText = Color.parseColor("#1C1E21")
    private val colorSub = Color.parseColor("#6B7280")
    private val colorAccent = Color.parseColor("#1A5FB4")
    private val colorChipOff = Color.parseColor("#E4E6EA")

    private val heightButtons = mutableListOf<Pair<TextView, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.settings_title)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(colorBg)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16f), dp(16f), dp(16f), dp(24f))
        scroll.addView(root)

        // ── 입력 ──────────────────────────────────────────────
        root.addView(sectionTitle("입력"))
        val card1 = card()
        card1.addView(switchRow("키 입력음", "키를 누를 때 소리를 냅니다.",
            Settings.soundEnabled(this)) { Settings.setBool(this, Settings.KEY_SOUND, it) })
        card1.addView(divider())
        card1.addView(switchRow("진동", "키를 누를 때 짧게 진동합니다.",
            Settings.vibrationEnabled(this)) { Settings.setBool(this, Settings.KEY_VIBRATION, it) })
        card1.addView(divider())
        card1.addView(switchRow("추천 바 표시", "한글 입력 시 영어 학습 추천을 띄웁니다.",
            Settings.suggestionsEnabled(this)) { Settings.setBool(this, Settings.KEY_SUGGESTIONS, it) })
        root.addView(card1)

        // ── 모양 ──────────────────────────────────────────────
        root.addView(sectionTitle("모양"))
        val card2 = card()
        card2.addView(rowLabel("키보드 높이", "손 크기나 화면에 맞춰 자판 높이를 조절합니다."))
        card2.addView(heightSelector())
        root.addView(card2)

        // ── 사전 출처·라이선스 ─────────────────────────────────
        root.addView(sectionTitle("사전 출처·라이선스"))
        val card3 = card()
        card3.addView(bodyText(getString(R.string.license_body)))
        card3.addView(linkText("한국어기초사전 (krdict.korean.go.kr) 열기",
            "https://krdict.korean.go.kr/"))
        card3.addView(linkText("CC BY-SA 2.0 KR 라이선스 전문 보기",
            "https://creativecommons.org/licenses/by-sa/2.0/kr/"))
        root.addView(card3)

        // ── 정보 ──────────────────────────────────────────────
        root.addView(sectionTitle("정보"))
        val card4 = card()
        val ver = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (e: Exception) { "" }
        // 정식 명칭 + 슬로건, 그리고 버전.
        card4.addView(rowLabel(getString(R.string.app_full_name), getString(R.string.app_slogan)))
        card4.addView(divider())
        card4.addView(rowLabel("버전", ver))
        root.addView(card4)

        setContentView(scroll)
    }

    // ── 화면 부품 도우미 ──────────────────────────────────────

    private fun sectionTitle(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(colorAccent)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setPadding(dp(4f), dp(18f), dp(4f), dp(6f))
        return tv
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setBackgroundColor(colorCard)
        c.setPadding(dp(16f), dp(6f), dp(16f), dp(6f))
        return c
    }

    private fun divider(): View {
        val v = View(this)
        v.setBackgroundColor(Color.parseColor("#EBECEF"))
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
        return v
    }

    /** 왼쪽에 제목/설명, 오른쪽에 켜기 스위치가 있는 한 줄. */
    private fun switchRow(
        title: String, desc: String, initial: Boolean, onChange: (Boolean) -> Unit
    ): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(12f), 0, dp(12f))

        row.addView(rowLabel(title, desc),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val sw = Switch(this)
        sw.isChecked = initial
        sw.setOnCheckedChangeListener { _, checked -> onChange(checked) }
        row.addView(sw)
        return row
    }

    /** 제목 + (선택) 회색 설명 두 줄짜리 라벨. */
    private fun rowLabel(title: String, desc: String?): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, dp(10f), 0, dp(10f))

        val t = TextView(this)
        t.text = title
        t.setTextColor(colorText)
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        box.addView(t)

        if (!desc.isNullOrBlank()) {
            val d = TextView(this)
            d.text = desc
            d.setTextColor(colorSub)
            d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            d.setPadding(0, dp(2f), 0, 0)
            box.addView(d)
        }
        return box
    }

    private fun bodyText(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(colorSub)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.setLineSpacing(dp(3f).toFloat(), 1f)
        tv.setPadding(0, dp(10f), 0, dp(8f))
        return tv
    }

    private fun linkText(label: String, url: String): TextView {
        val tv = TextView(this)
        tv.text = label
        tv.setTextColor(colorAccent)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.setPadding(0, dp(10f), 0, dp(10f))
        tv.isClickable = true
        tv.setOnClickListener { openUrl(url) }
        return tv
    }

    /** 낮음/보통/높음 3단계 선택 버튼. */
    private fun heightSelector(): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, dp(4f), 0, dp(12f))

        heightButtons.clear()
        val items = listOf(
            "낮음" to Settings.HEIGHT_LOW,
            "보통" to Settings.HEIGHT_NORMAL,
            "높음" to Settings.HEIGHT_HIGH,
        )
        for ((label, value) in items) {
            val tv = TextView(this)
            tv.text = label
            tv.gravity = Gravity.CENTER
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            tv.setPadding(0, dp(12f), 0, dp(12f))
            tv.isClickable = true
            tv.setOnClickListener {
                Settings.setHeight(this, value)
                refreshHeightButtons()
            }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = dp(4f); lp.marginEnd = dp(4f)
            row.addView(tv, lp)
            heightButtons.add(tv to value)
        }
        refreshHeightButtons()
        return row
    }

    private fun refreshHeightButtons() {
        val current = Settings.height(this)
        for ((tv, value) in heightButtons) {
            val selected = value == current
            tv.setTextColor(if (selected) Color.WHITE else colorText)
            tv.setBackgroundColor(if (selected) colorAccent else colorChipOff)
        }
    }

    private fun openUrl(url: String) {
        // 외부 브라우저 앱을 여는 것뿐입니다. 이 앱 자체는 네트워크를 쓰지 않습니다.
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // 브라우저가 없는 경우 등 — 조용히 무시
        }
    }
}
