package io.github.zama47.asthmahelper.domain.usecase

import io.github.zama47.asthmahelper.domain.model.DutyMeasurement
import io.github.zama47.asthmahelper.domain.repository.DutyMeasurementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Возвращает замеры за период [startTime, endTime]. */
class GetMeasurementsForPeriodUseCase @Inject constructor(private val repository: DutyMeasurementRepository) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<DutyMeasurement>> =
        repository.getMeasurementsForDay(startTime, endTime)
}
