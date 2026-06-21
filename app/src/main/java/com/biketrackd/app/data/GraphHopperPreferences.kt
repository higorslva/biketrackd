package com.biketrackd.app.data

import android.content.Context

object GraphHopperPreferences {
    private const val PREFS_NAME = "graphhopper_prefs"
    private const val KEY_API_KEY = "api_key"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String {
        return prefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_API_KEY, key).apply()
    }
}
