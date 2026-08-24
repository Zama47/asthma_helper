package com.example.asthmahelper.domain.repository

import com.example.asthmahelper.domain.model.MedicationLog
import kotlinx.coroutines.flow.Flow

interface MedicationLogRepository {
    fun getLogsForDate(date: Long): Flow<List<MedicationLog>>
    suspend fun getLogForScheduleAndDate(scheduleId: Long, date: Long): MedicationLog?
    fun getLogsForSchedule(scheduleId: Long): Flow<List<MedicationLog>>
    fun getLogsForPeriod(startDate: Long, endDate: Long): Flow<List<MedicationLog>>
    suspend fun insertLog(log: MedicationLog): Long
    suspend fun updateLog(log: MedicationLog)
    suspend fun deleteLog(log: MedicationLog)
}