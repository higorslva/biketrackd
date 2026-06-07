package com.biketrackd.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PedalSessionDao {

    @Insert
    suspend fun insert(session: PedalSession)

    @Query("SELECT * FROM pedal_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<PedalSession>>

    @Query("SELECT COUNT(*) FROM pedal_history")
    fun getSessionCountFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalDistance), 0) FROM pedal_history")
    fun getTotalDistanceFlow(): Flow<Float>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM pedal_history")
    fun getTotalDurationFlow(): Flow<Long>

    @Query("SELECT COALESCE(AVG(avgSpeed), 0) FROM pedal_history")
    fun getOverallAvgSpeedFlow(): Flow<Float>

    // Records
    @Query("SELECT COALESCE(MAX(totalDistance), 0) FROM pedal_history")
    fun getBestDistanceFlow(): Flow<Float>

    @Query("SELECT COALESCE(MAX(durationSeconds), 0) FROM pedal_history")
    fun getBestDurationFlow(): Flow<Long>

    @Query("SELECT COALESCE(MAX(avgSpeed), 0) FROM pedal_history")
    fun getBestAvgSpeedFlow(): Flow<Float>

    // Weekly distances (ISO week, last 8)
    @Query("""
        SELECT strftime('%Y-%W', timestamp / 1000, 'unixepoch') AS week,
               SUM(totalDistance) AS dist
        FROM pedal_history
        GROUP BY week ORDER BY week DESC LIMIT 8
    """)
    fun getWeeklyDistances(): Flow<List<WeekDist>>

    // Monthly distances (last 12)
    @Query("""
        SELECT strftime('%Y-%m', timestamp / 1000, 'unixepoch') AS month,
               SUM(totalDistance) AS dist
        FROM pedal_history
        GROUP BY month ORDER BY month DESC LIMIT 12
    """)
    fun getMonthlyDistances(): Flow<List<MonthDist>>

    // Distinct timestamps for streak
    @Query("SELECT DISTINCT timestamp FROM pedal_history ORDER BY timestamp DESC")
    fun getAllTimestamps(): Flow<List<Long>>
}
