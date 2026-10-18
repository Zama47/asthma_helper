package io.github.zama47.asthmahelper.domain.usecase

import io.github.zama47.asthmahelper.domain.model.DutyMeasurement
import io.github.zama47.asthmahelper.domain.repository.DutyMeasurementRepository
import javax.inject.Inject

/** Добавляет новый замер пикфлоуметрии. */
class AddMeasurementUseCase @Inject constructor(private val repository: DutyMeasurementRepository) {
    suspend operator fun invoke(value: Float, note: String? = null): Long {
        require(value > 0f) { "Значение замера должно быть положительным" }
        return repository.insertMeasurement(
            DutyMeasurement(
                value = value,
                timestamp = System.currentTimeMillis(),
                note = note?.takeIf { it.isNotBlank() }
            )
        )
    }
}
