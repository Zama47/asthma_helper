package io.github.zama47.asthmahelper.domain.repository

import io.github.zama47.asthmahelper.domain.model.MedicationSchedule
import kotlinx.coroutines.flow.Flow

interface MedicationScheduleRepository {
    fun getSchedulesForMedicine(medicineId: Long): Flow<List<MedicationSchedule>>
    fun getActiveSchedules(): Flow<List<MedicationSchedule>>
    suspend fun getScheduleById(id: Long): MedicationSchedule?
    suspend fun insertSchedule(schedule: MedicationSchedule): Long
    suspend fun updateSchedule(schedule: MedicationSchedule)
    suspend fun deleteSchedule(schedule: MedicationSchedule)
}
