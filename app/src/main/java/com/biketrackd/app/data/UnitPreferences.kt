package com.biketrackd.app.data

import android.content.Context
import android.content.SharedPreferences

object UnitPreferences {
    private const val PREFS_NAME = "unit_prefs"
    private const val KEY_UNIT_SYSTEM = "unit_system"

    enum class UnitSystem { METRIC, IMPERIAL }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(context: Context): UnitSystem {
        val ordinal = prefs(context).getInt(KEY_UNIT_SYSTEM, 0)
        return UnitSystem.entries.getOrElse(ordinal) { UnitSystem.METRIC }
    }

    fun set(context: Context, system: UnitSystem) {
        prefs(context).edit().putInt(KEY_UNIT_SYSTEM, system.ordinal).apply()
    }
}
