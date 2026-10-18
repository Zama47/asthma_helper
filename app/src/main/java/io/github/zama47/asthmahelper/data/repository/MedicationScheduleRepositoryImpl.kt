package io.github.zama47.asthmahelper.data.repository

import io.github.zama47.asthmahelper.data.db.dao.MedicationScheduleDao
import io.github.zama47.asthmahelper.data.db.entity.MedicationScheduleEntity
import io.github.zama47.asthmahelper.domain.model.MedicationSchedule
import io.github.zama47.asthmahelper.domain.repository.MedicationScheduleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicationScheduleRepositoryImpl @Inject constructor(private val dao: MedicationScheduleDao) :
    MedicationScheduleRepository {

    override fun getSchedulesForMedicine(medicineId: Long): Flow<List<MedicationSchedule>> =
        dao.getSchedulesForMedicine(medicineId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getActiveSchedules(): Flow<List<MedicationSchedule>> = dao.getActiveSchedules().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getScheduleById(id: Long): MedicationSchedule? = dao.getScheduleById(id)?.toDomain()

    override suspend fun insertSchedule(schedule: MedicationSchedule): Long = dao.insert(schedule.toEntity())

    override suspend fun updateSchedule(schedule: MedicationSchedule) {
        dao.update(schedule.toEntity())
    }

    override suspend fun deleteSchedule(schedule: MedicationSchedule) {
        dao.delete(schedule.toEntity())
    }

    private fun MedicationScheduleEntity.toDomain(): MedicationSchedule = MedicationSchedule(
        id = id,
        medicineId = medicineId,
        dose = dose,
        timeOfDay = timeOfDay,
        daysOfWeek = daysOfWeek,
        isActive = isActive
    )

    private fun MedicationSchedule.toEntity(): MedicationScheduleEntity = MedicationScheduleEntity(
        id = id,
        medicineId = medicineId,
        dose = dose,
        timeOfDay = timeOfDay,
        daysOfWeek = daysOfWeek,
        isActive = isActive
    )
}
