package com.example.asthmahelper.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.asthmahelper.data.db.entity.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MedicationLogEntity): Long

    @Update
    suspend fun update(log: MedicationLogEntity)

    @Delete
    suspend fun delete(log: MedicationLogEntity)

    @Query("SELECT * FROM medication_logs WHERE date = :date")
    fun getLogsForDate(date: Long): Flow<List<MedicationLogEntity>>

    @Query(
        "SELECT * FROM medication_logs " +
            "WHERE scheduleId = :scheduleId AND date = :date " +
            "LIMIT 1"
    )
    suspend fun getLogForScheduleAndDate(scheduleId: Long, date: Long): MedicationLogEntity?

    /** Реактивный вариант: эмитит заново при любом изменении таблицы логов. */
    @Query(
        "SELECT * FROM medication_logs " +
            "WHERE scheduleId = :scheduleId AND date = :date " +
            "LIMIT 1"
    )
    fun getLogForScheduleAndDateFlow(scheduleId: Long, date: Long): Flow<MedicationLogEntity?>

    @Query("SELECT * FROM medication_logs WHERE scheduleId = :scheduleId ORDER BY date")
    fun getLogsForSchedule(scheduleId: Long): Flow<List<MedicationLogEntity>>

    @Query(
        "SELECT * FROM medication_logs " +
            "WHERE date BETWEEN :startDate AND :endDate ORDER BY date"
    )
    fun getLogsForPeriod(startDate: Long, endDate: Long): Flow<List<MedicationLogEntity>>
}
