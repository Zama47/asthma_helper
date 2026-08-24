package com.example.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.asthmahelper.data.db.entity.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: MedicationScheduleEntity): Long

    @Update
    suspend fun update(schedule: MedicationScheduleEntity)

    @Delete
    suspend fun delete(schedule: MedicationScheduleEntity)

    @Query("SELECT * FROM medication_schedules WHERE medicineId = :medicineId ORDER BY timeOfDay")
    fun getSchedulesForMedicine(medicineId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE isActive = 1 ORDER BY timeOfDay")
    fun getActiveSchedules(): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): MedicationScheduleEntity?
}
