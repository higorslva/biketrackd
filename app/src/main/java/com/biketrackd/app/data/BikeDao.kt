package com.biketrackd.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BikeDao {

    @Query("SELECT * FROM bike ORDER BY isDefault DESC, id ASC")
    fun getAllFlow(): Flow<List<Bike>>

    @Query("SELECT * FROM bike WHERE isDefault = 1 LIMIT 1")
    fun getDefaultFlow(): Flow<Bike?>

    @Query("SELECT * FROM bike WHERE id = :id")
    suspend fun getById(id: Long): Bike?

    @Insert
    suspend fun insert(bike: Bike): Long

    @Update
    suspend fun update(bike: Bike)

    @Query("UPDATE bike SET isDefault = 0")
    suspend fun clearDefault()

    @Query("DELETE FROM bike WHERE id = :id")
    suspend fun deleteById(id: Long)
}
