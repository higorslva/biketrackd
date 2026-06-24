package com.biketrackd.app.data

import android.content.Context
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    private const val BACKUP_VERSION = 1

    suspend fun export(context: Context): String {
        val db = AppDatabase.getInstance(context)
        val sessions = db.pedalSessionDao().getAllFlow().first()
        val bikes = db.bikeDao().getAllFlow().first()
        val parts = db.maintenancePartDao().getAllPartsFlow().first()

        val bikeIdToName = bikes.associate { it.id to it.name }

        val json = JSONObject()
        json.put("version", BACKUP_VERSION)
        json.put("exportDate", System.currentTimeMillis())

        val dbObj = JSONObject()

        val bikesArr = JSONArray()
        for (b in bikes) {
            val obj = JSONObject()
            obj.put("name", b.name)
            obj.put("model", b.model)
            obj.put("type", b.type)
            obj.put("acquisitionDate", b.acquisitionDate)
            obj.put("notes", b.notes)
            obj.put("isDefault", b.isDefault)
            bikesArr.put(obj)
        }
        dbObj.put("bikes", bikesArr)

        val partsArr = JSONArray()
        for (p in parts) {
            val obj = JSONObject()
            obj.put("bikeName", bikeIdToName[p.bikeId] ?: "")
            obj.put("name", p.name)
            obj.put("componentType", p.componentType)
            obj.put("lifespanKm", p.lifespanKm.toDouble())
            obj.put("usedKm", p.usedKm.toDouble())
            obj.put("installDate", p.installDate)
            obj.put("notes", p.notes)
            partsArr.put(obj)
        }
        dbObj.put("maintenanceParts", partsArr)

        val sessionsArr = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("timestamp", s.timestamp)
            obj.put("totalDistance", s.totalDistance.toDouble())
            obj.put("maxSpeed", s.maxSpeed.toDouble())
            obj.put("avgSpeed", s.avgSpeed.toDouble())
            obj.put("durationSeconds", s.durationSeconds)
            obj.put("trailData", s.trailData ?: "")
            obj.put("bikeName", s.bikeId?.let { bikeIdToName[it] } ?: "")
            sessionsArr.put(obj)
        }
        dbObj.put("sessions", sessionsArr)

        json.put("database", dbObj)

        val prefs = JSONObject()
        prefs.put("speed_limit_enabled", SpeedLimitPreferences.isEnabled(context))
        prefs.put("speed_limit_value", SpeedLimitPreferences.getLimit(context))
        prefs.put("gh_api_key", GraphHopperPreferences.getApiKey(context))
        prefs.put("theme_mode", ThemePreferences.get(context).ordinal)
        prefs.put("language", LanguagePreferences.get(context))
        prefs.put("unit_system", UnitPreferences.get(context).ordinal)
        prefs.put("orientation", OrientationPreferences.get(context).ordinal)
        json.put("preferences", prefs)

        return json.toString(2)
    }

    suspend fun import(context: Context, json: String): Result<Unit> = runCatching {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)
        if (version != BACKUP_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        val db = AppDatabase.getInstance(context)

        // Restore preferences (optXxx for forward compatibility)
        val prefs = root.optJSONObject("preferences")
        if (prefs != null) {
            if (prefs.has("speed_limit_enabled"))
                SpeedLimitPreferences.setEnabled(context, prefs.optBoolean("speed_limit_enabled", false))
            if (prefs.has("speed_limit_value"))
                SpeedLimitPreferences.setLimit(context, prefs.optInt("speed_limit_value", 20))
            if (prefs.has("gh_api_key"))
                GraphHopperPreferences.setApiKey(context, prefs.optString("gh_api_key", ""))
            if (prefs.has("theme_mode")) {
                val mode = ThemePreferences.ThemeMode.entries.getOrElse(prefs.optInt("theme_mode", 0)) { ThemePreferences.ThemeMode.SYSTEM }
                ThemePreferences.set(context, mode)
            }
            if (prefs.has("language"))
                LanguagePreferences.set(context, prefs.optString("language", ""))
            if (prefs.has("unit_system")) {
                val unit = UnitPreferences.UnitSystem.entries.getOrElse(prefs.optInt("unit_system", 0)) { UnitPreferences.UnitSystem.METRIC }
                UnitPreferences.set(context, unit)
            }
            if (prefs.has("orientation")) {
                val orient = OrientationPreferences.Orientation.entries.getOrElse(prefs.optInt("orientation", 2)) { OrientationPreferences.Orientation.AUTOMATIC }
                OrientationPreferences.set(context, orient)
            }
        }

        // Restore database
        val dbObj = root.optJSONObject("database") ?: return@runCatching

        // 1. Bikes first — build name→id map
        val bikeNameToId = mutableMapOf<String, Long>()
        val bikesArr = dbObj.optJSONArray("bikes")
        if (bikesArr != null) {
            for (i in 0 until bikesArr.length()) {
                val obj = bikesArr.getJSONObject(i)
                val bike = Bike(
                    name = obj.optString("name", ""),
                    model = obj.optString("model", ""),
                    type = obj.optString("type", ""),
                    acquisitionDate = obj.optLong("acquisitionDate", 0L),
                    notes = obj.optString("notes", ""),
                    isDefault = obj.optBoolean("isDefault", false),
                )
                if (bike.name.isNotBlank()) {
                    val newId = db.bikeDao().insert(bike)
                    bikeNameToId[bike.name] = newId
                }
            }
        }

        // 2. Maintenance parts
        val partsArr = dbObj.optJSONArray("maintenanceParts")
        if (partsArr != null) {
            for (i in 0 until partsArr.length()) {
                val obj = partsArr.getJSONObject(i)
                val bikeName = obj.optString("bikeName", "")
                val bikeId = bikeNameToId[bikeName] ?: 0L
                val part = MaintenancePart(
                    bikeId = bikeId,
                    name = obj.optString("name", ""),
                    componentType = obj.optString("componentType", "OTHER"),
                    lifespanKm = obj.optDouble("lifespanKm", 0.0).toFloat(),
                    usedKm = obj.optDouble("usedKm", 0.0).toFloat(),
                    installDate = obj.optLong("installDate", 0L),
                    notes = obj.optString("notes", ""),
                )
                if (part.name.isNotBlank() && part.lifespanKm > 0f) {
                    db.maintenancePartDao().insert(part)
                }
            }
        }

        // 3. Sessions
        val sessionsArr = dbObj.optJSONArray("sessions")
        if (sessionsArr != null) {
            for (i in 0 until sessionsArr.length()) {
                val obj = sessionsArr.getJSONObject(i)
                val bikeName = obj.optString("bikeName", "")
                val session = PedalSession(
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    totalDistance = obj.optDouble("totalDistance", 0.0).toFloat(),
                    maxSpeed = obj.optDouble("maxSpeed", 0.0).toFloat(),
                    avgSpeed = obj.optDouble("avgSpeed", 0.0).toFloat(),
                    durationSeconds = obj.optLong("durationSeconds", 0L),
                    trailData = obj.optString("trailData", "").ifBlank { null },
                    bikeId = bikeNameToId[bikeName],
                )
                if (session.totalDistance > 0f) {
                    db.pedalSessionDao().insert(session)
                }
            }
        }
    }
}
