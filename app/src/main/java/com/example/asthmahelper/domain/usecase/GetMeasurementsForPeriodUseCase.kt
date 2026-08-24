package com.example.asthmahelper.domain.usecase

import com.example.asthmahelper.domain.model.DutyMeasurement
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Возвращает замеры за период [startTime, endTime]. */
class GetMeasurementsForPeriodUseCase @Inject constructor(
    private val repository: DutyMeasurementRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<DutyMeasurement>> {
        return repository.getMeasurementsForDay(startTime, endTime)
    }
}
