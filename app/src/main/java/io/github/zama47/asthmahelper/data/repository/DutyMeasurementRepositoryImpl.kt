package io.github.zama47.asthmahelper.data.repository

import io.github.zama47.asthmahelper.data.db.dao.DutyMeasurementDao
import io.github.zama47.asthmahelper.data.db.entity.DutyMeasurementEntity
import io.github.zama47.asthmahelper.domain.model.DutyMeasurement
import io.github.zama47.asthmahelper.domain.repository.DutyMeasurementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DutyMeasurementRepositoryImpl @Inject constructor(private val dao: DutyMeasurementDao) :
    DutyMeasurementRepository {

    override fun getAllMeasurements(): Flow<List<DutyMeasurement>> = dao.getAllMeasurements().map { entities ->
        entities.map { entity ->
            entity.toDomain()
        }
    }

    override fun getMeasurementsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DutyMeasurement>> =
        dao.getMeasurementsForDay(startOfDay, endOfDay).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override suspend fun getLatestMeasurement(): DutyMeasurement? = dao.getLatestMeasurement()?.toDomain()

    override suspend fun insertMeasurement(measurement: DutyMeasurement): Long = dao.insert(measurement.toEntity())

    override suspend fun updateMeasurement(measurement: DutyMeasurement) {
        dao.update(measurement.toEntity())
    }

    override suspend fun deleteMeasurement(measurement: DutyMeasurement) {
        dao.delete(measurement.toEntity())
    }

    private fun DutyMeasurementEntity.toDomain(): DutyMeasurement = DutyMeasurement(
        id = id,
        value = value,
        timestamp = timestamp,
        note = note
    )

    private fun DutyMeasurement.toEntity(): DutyMeasurementEntity = DutyMeasurementEntity(
        id = id,
        value = value,
        timestamp = timestamp,
        note = note
    )
}
