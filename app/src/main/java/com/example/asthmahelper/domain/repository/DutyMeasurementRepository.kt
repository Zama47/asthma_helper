package com.example.asthmahelper.domain.repository

import com.example.asthmahelper.domain.model.DutyMeasurement
import kotlinx.coroutines.flow.Flow

interface DutyMeasurementRepository {
    fun getAllMeasurements(): Flow<List<DutyMeasurement>>
    fun getMeasurementsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DutyMeasurement>>
    suspend fun getLatestMeasurement(): DutyMeasurement?
    suspend fun insertMeasurement(measurement: DutyMeasurement): Long
    suspend fun updateMeasurement(measurement: DutyMeasurement)
    suspend fun deleteMeasurement(measurement: DutyMeasurement)
}