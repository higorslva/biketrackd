package com.biketrackd.app.data

import android.content.Context

object OrientationPreferences {
    private const val PREFS_NAME = "orientation_prefs"
    private const val KEY_ORIENTATION = "orientation"

    enum class Orientation { LANDSCAPE, PORTRAIT, AUTOMATIC }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(context: Context): Orientation {
        val ordinal = prefs(context).getInt(KEY_ORIENTATION, 2)
        return Orientation.entries.getOrElse(ordinal) { Orientation.LANDSCAPE }
    }

    fun set(context: Context, orientation: Orientation) {
        prefs(context).edit().putInt(KEY_ORIENTATION, orientation.ordinal).apply()
    }
}
