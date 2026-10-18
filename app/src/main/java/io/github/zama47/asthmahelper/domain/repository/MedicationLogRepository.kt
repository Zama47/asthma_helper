package io.github.zama47.asthmahelper.domain.repository

import io.github.zama47.asthmahelper.domain.model.MedicationLog
import kotlinx.coroutines.flow.Flow

interface MedicationLogRepository {
    fun getLogsForDate(date: Long): Flow<List<MedicationLog>>
    suspend fun getLogForScheduleAndDate(scheduleId: Long, date: Long): MedicationLog?
    fun getLogForScheduleAndDateFlow(scheduleId: Long, date: Long): Flow<MedicationLog?>
    fun getLogsForSchedule(scheduleId: Long): Flow<List<MedicationLog>>
    fun getLogsForPeriod(startDate: Long, endDate: Long): Flow<List<MedicationLog>>
    suspend fun insertLog(log: MedicationLog): Long
    suspend fun updateLog(log: MedicationLog)
    suspend fun deleteLog(log: MedicationLog)
}
