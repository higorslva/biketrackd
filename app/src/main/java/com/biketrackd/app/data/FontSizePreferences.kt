package com.biketrackd.app.data

import android.content.Context

object FontSizePreferences {
    private const val PREFS_NAME = "fontsize_prefs"
    private const val KEY_SCALE = "font_scale"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFontScale(context: Context): Float =
        prefs(context).getFloat(KEY_SCALE, 1.0f)

    fun setFontScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(KEY_SCALE, scale).apply()
    }
}
