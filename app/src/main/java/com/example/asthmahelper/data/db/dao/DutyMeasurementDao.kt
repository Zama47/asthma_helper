package com.example.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.asthmahelper.data.db.entity.DutyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DutyMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: DutyMeasurementEntity): Long

    @Update
    suspend fun update(measurement: DutyMeasurementEntity)

    @Delete
    suspend fun delete(measurement: DutyMeasurementEntity)

    @Query("SELECT * FROM duty_measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<DutyMeasurementEntity>>

    @Query(
        "SELECT * FROM duty_measurements " +
            "WHERE timestamp BETWEEN :startOfDay AND :endOfDay " +
            "ORDER BY timestamp ASC"
    )
    fun getMeasurementsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DutyMeasurementEntity>>

    @Query(
        "SELECT * FROM duty_measurements " +
            "WHERE timestamp BETWEEN :startTime AND :endTime " +
            "ORDER BY timestamp DESC LIMIT 1"
    )
    suspend fun getLatestMeasurementInPeriod(startTime: Long, endTime: Long): DutyMeasurementEntity?

    @Query("SELECT * FROM duty_measurements ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMeasurement(): DutyMeasurementEntity?
}
