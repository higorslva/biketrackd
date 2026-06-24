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

    @Query("SELECT COALESCE(MAX(maxSpeed), 0) FROM pedal_history")
    fun getBestMaxSpeedFlow(): Flow<Float>

    // Record session details
    @Query("SELECT * FROM pedal_history ORDER BY totalDistance DESC LIMIT 1")
    fun getBestDistanceSessionFlow(): Flow<PedalSession?>

    @Query("SELECT * FROM pedal_history ORDER BY durationSeconds DESC LIMIT 1")
    fun getBestDurationSessionFlow(): Flow<PedalSession?>

    @Query("SELECT * FROM pedal_history ORDER BY avgSpeed DESC LIMIT 1")
    fun getBestAvgSpeedSessionFlow(): Flow<PedalSession?>

    @Query("SELECT * FROM pedal_history ORDER BY maxSpeed DESC LIMIT 1")
    fun getBestMaxSpeedSessionFlow(): Flow<PedalSession?>

    @Query("SELECT * FROM pedal_history ORDER BY totalDistance DESC LIMIT 1")
    fun getBestSessionFlow(): Flow<PedalSession?>

    // Monthly range queries
    @Query("SELECT COALESCE(SUM(totalDistance), 0) FROM pedal_history WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun getRangeDistanceFlow(startMillis: Long, endMillis: Long): Flow<Float>

    @Query("SELECT COUNT(*) FROM pedal_history WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun getRangeSessionCountFlow(startMillis: Long, endMillis: Long): Flow<Int>

    // Delete
    @Query("DELETE FROM pedal_history WHERE id = :id")
    suspend fun deleteById(id: Long)

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
