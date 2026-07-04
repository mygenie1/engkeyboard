package com.hanyeong.keyboard

import android.content.Context

/**
 * 앱 설정값을 한곳에서 읽고 쓰는 도우미입니다.
 *
 * 저장은 안드로이드 기본 저장소(SharedPreferences)에 합니다. 네트워크와 무관하며,
 * 값은 기기 안에만 남습니다. 설정 화면(SettingsActivity)에서 바꾸고,
 * 키보드(KoreanImeService/KoreanKeyboardView)에서 읽어 동작에 반영합니다.
 */
object Settings {
    private const val PREFS = "keyboard_settings"

    const val KEY_SOUND = "sound"               // 키 입력음
    const val KEY_VIBRATION = "vibration"        // 진동(햅틱)
    const val KEY_SUGGESTIONS = "suggestions"    // 추천 바 표시
    const val KEY_HEIGHT = "height"              // 키보드 높이

    const val HEIGHT_LOW = "low"
    const val HEIGHT_NORMAL = "normal"
    const val HEIGHT_HIGH = "high"

    private fun prefs(c: Context) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // 기본값: 소리·진동은 꺼짐, 추천은 켜짐, 높이는 보통.
    fun soundEnabled(c: Context) = prefs(c).getBoolean(KEY_SOUND, false)
    fun vibrationEnabled(c: Context) = prefs(c).getBoolean(KEY_VIBRATION, false)
    fun suggestionsEnabled(c: Context) = prefs(c).getBoolean(KEY_SUGGESTIONS, true)
    fun height(c: Context): String = prefs(c).getString(KEY_HEIGHT, HEIGHT_NORMAL) ?: HEIGHT_NORMAL

    /** 높이 설정을 키 높이 배율로 바꿉니다. (보통=1.0 기준) */
    fun heightScale(c: Context): Float = when (height(c)) {
        HEIGHT_LOW -> 0.85f
        HEIGHT_HIGH -> 1.18f
        else -> 1.0f
    }

    fun setBool(c: Context, key: String, value: Boolean) =
        prefs(c).edit().putBoolean(key, value).apply()

    fun setHeight(c: Context, value: String) =
        prefs(c).edit().putString(KEY_HEIGHT, value).apply()
}
