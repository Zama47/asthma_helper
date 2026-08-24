package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.MedicationScheduleDao
import com.example.asthmahelper.data.db.entity.MedicationScheduleEntity
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicationScheduleRepositoryImpl @Inject constructor(
    private val dao: MedicationScheduleDao
) : MedicationScheduleRepository {

    override fun getSchedulesForMedicine(medicineId: Long): Flow<List<MedicationSchedule>> {
        return dao.getSchedulesForMedicine(medicineId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveSchedules(): Flow<List<MedicationSchedule>> {
        return dao.getActiveSchedules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getScheduleById(id: Long): MedicationSchedule? {
        return dao.getScheduleById(id)?.toDomain()
    }

    override suspend fun insertSchedule(schedule: MedicationSchedule): Long {
        return dao.insert(schedule.toEntity())
    }

    override suspend fun updateSchedule(schedule: MedicationSchedule) {
        dao.update(schedule.toEntity())
    }

    override suspend fun deleteSchedule(schedule: MedicationSchedule) {
        dao.delete(schedule.toEntity())
    }

    private fun MedicationScheduleEntity.toDomain(): MedicationSchedule {
        return MedicationSchedule(
            id = id,
            medicineId = medicineId,
            dose = dose,
            timeOfDay = timeOfDay,
            daysOfWeek = daysOfWeek,
            isActive = isActive
        )
    }

    private fun MedicationSchedule.toEntity(): MedicationScheduleEntity {
        return MedicationScheduleEntity(
            id = id,
            medicineId = medicineId,
            dose = dose,
            timeOfDay = timeOfDay,
            daysOfWeek = daysOfWeek,
            isActive = isActive
        )
    }
}