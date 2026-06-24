package com.biketrackd.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenancePartDao {

    @Query("SELECT * FROM maintenance_part WHERE bikeId = :bikeId")
    fun getPartsByBikeFlow(bikeId: Long): Flow<List<MaintenancePart>>

    @Query("""
        SELECT mp.* FROM maintenance_part mp
        JOIN bike b ON b.id = mp.bikeId
        ORDER BY b.id, mp.componentType, mp.id
    """)
    fun getAllPartsFlow(): Flow<List<MaintenancePart>>

    @Query("SELECT * FROM maintenance_part WHERE id = :id")
    suspend fun getById(id: Long): MaintenancePart?

    @Insert
    suspend fun insert(part: MaintenancePart): Long

    @Update
    suspend fun update(part: MaintenancePart)

    @Query("DELETE FROM maintenance_part WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE maintenance_part SET usedKm = usedKm + :km WHERE bikeId = :bikeId")
    suspend fun addKmToBikeParts(bikeId: Long, km: Float)

    @Query("SELECT COUNT(*) FROM maintenance_part WHERE usedKm >= lifespanKm * 0.8 AND lifespanKm > 0")
    fun getWornCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM maintenance_part WHERE usedKm >= lifespanKm * 0.9 AND lifespanKm > 0")
    fun getWornCriticalFlow(): Flow<Int>
}
