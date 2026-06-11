package com.biketrackd.app.data

import android.content.Context

object SpeedLimitPreferences {

    private const val PREFS_NAME = "speed_limit_prefs"
    private const val KEY_ENABLED = "speed_limit_enabled"
    private const val KEY_LIMIT = "speed_limit_value"
    private const val DEFAULT_LIMIT = 20

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun getLimit(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LIMIT, DEFAULT_LIMIT)
    }

    fun setLimit(context: Context, limit: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LIMIT, limit)
            .apply()
    }
}
