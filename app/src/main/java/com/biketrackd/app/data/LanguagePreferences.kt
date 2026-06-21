package com.biketrackd.app.data

import android.content.Context

object LanguagePreferences {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(context: Context): String {
        return prefs(context).getString(KEY_LANGUAGE, "") ?: ""
    }

    fun set(context: Context, lang: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, lang).apply()
    }
}
