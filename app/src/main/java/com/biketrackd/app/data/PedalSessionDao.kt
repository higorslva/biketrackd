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
}
