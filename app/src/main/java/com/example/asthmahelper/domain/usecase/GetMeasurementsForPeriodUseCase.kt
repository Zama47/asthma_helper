package com.example.asthmahelper.domain.usecase

import com.example.asthmahelper.domain.model.DutyMeasurement
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Возвращает замеры за период [startTime, endTime]. */
class GetMeasurementsForPeriodUseCase @Inject constructor(private val repository: DutyMeasurementRepository) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<DutyMeasurement>> =
        repository.getMeasurementsForDay(startTime, endTime)
}
