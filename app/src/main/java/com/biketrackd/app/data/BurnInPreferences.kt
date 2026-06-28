package com.biketrackd.app.data

import android.content.Context

object BurnInPreferences {
    private const val PREFS_NAME = "burnin_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DIMMING = "dimming"
    private const val KEY_DIM_TEXT = "dim_text"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isDimmingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DIMMING, true)

    fun setDimmingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIMMING, enabled).apply()
    }

    fun isDimTextEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DIM_TEXT, true)

    fun setDimTextEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIM_TEXT, enabled).apply()
    }
}
