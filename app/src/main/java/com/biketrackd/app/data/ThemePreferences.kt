package com.biketrackd.app.data

import android.content.Context

object ThemePreferences {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(context: Context): ThemeMode {
        val ordinal = prefs(context).getInt(KEY_THEME_MODE, 0)
        return ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    fun set(context: Context, mode: ThemeMode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }
}
