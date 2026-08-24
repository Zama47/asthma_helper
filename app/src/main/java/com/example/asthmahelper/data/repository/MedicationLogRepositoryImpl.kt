package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.MedicationLogDao
import com.example.asthmahelper.data.db.entity.MedicationLogEntity
import com.example.asthmahelper.domain.model.MedicationLog
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicationLogRepositoryImpl @Inject constructor(
    private val dao: MedicationLogDao
) : MedicationLogRepository {

    override fun getLogsForDate(date: Long): Flow<List<MedicationLog>> {
        return dao.getLogsForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLogForScheduleAndDate(scheduleId: Long, date: Long): MedicationLog? {
        return dao.getLogForScheduleAndDate(scheduleId, date)?.toDomain()
    }

    override fun getLogsForSchedule(scheduleId: Long): Flow<List<MedicationLog>> {
        return dao.getLogsForSchedule(scheduleId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsForPeriod(startDate: Long, endDate: Long): Flow<List<MedicationLog>> {
        return dao.getLogsForPeriod(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertLog(log: MedicationLog): Long {
        return dao.insert(log.toEntity())
    }

    override suspend fun updateLog(log: MedicationLog) {
        dao.update(log.toEntity())
    }

    override suspend fun deleteLog(log: MedicationLog) {
        dao.delete(log.toEntity())
    }

    private fun MedicationLogEntity.toDomain(): MedicationLog {
        return MedicationLog(
            id = id,
            scheduleId = scheduleId,
            date = date,
            taken = taken
        )
    }

    private fun MedicationLog.toEntity(): MedicationLogEntity {
        return MedicationLogEntity(
            id = id,
            scheduleId = scheduleId,
            date = date,
            taken = taken
        )
    }
}