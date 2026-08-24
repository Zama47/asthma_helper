package com.example.asthmahelper.data.repository

import com.example.asthmahelper.data.db.dao.DutyMeasurementDao
import com.example.asthmahelper.data.db.entity.DutyMeasurementEntity
import com.example.asthmahelper.domain.model.DutyMeasurement
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DutyMeasurementRepositoryImpl @Inject constructor(
    private val dao: DutyMeasurementDao
) : DutyMeasurementRepository {

    override fun getAllMeasurements(): Flow<List<DutyMeasurement>> {
        return dao.getAllMeasurements().map { entities ->
            entities.map { entity ->
                entity.toDomain()
            }
        }
    }

    override fun getMeasurementsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DutyMeasurement>> {
        return dao.getMeasurementsForDay(startOfDay, endOfDay).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun getLatestMeasurement(): DutyMeasurement? {
        return dao.getLatestMeasurement()?.toDomain()
    }

    override suspend fun insertMeasurement(measurement: DutyMeasurement): Long {
        return dao.insert(measurement.toEntity())
    }

    override suspend fun updateMeasurement(measurement: DutyMeasurement) {
        dao.update(measurement.toEntity())
    }

    override suspend fun deleteMeasurement(measurement: DutyMeasurement) {
        dao.delete(measurement.toEntity())
    }

    private fun DutyMeasurementEntity.toDomain(): DutyMeasurement {
        return DutyMeasurement(
            id = id,
            value = value,
            timestamp = timestamp,
            note = note
        )
    }

    private fun DutyMeasurement.toEntity(): DutyMeasurementEntity {
        return DutyMeasurementEntity(
            id = id,
            value = value,
            timestamp = timestamp,
            note = note
        )
    }
}