package com.example.asthmahelper.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.model.DutyMeasurement
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import com.example.asthmahelper.domain.usecase.SetBreathingNormUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dutyMeasurementRepository: DutyMeasurementRepository,
    private val breathingNormRepository: BreathingNormRepository,
    private val setBreathingNormUseCase: SetBreathingNormUseCase
) : ViewModel() {

    val measurements: StateFlow<List<DutyMeasurement>> =
        dutyMeasurementRepository.getAllMeasurements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _breathingNorm = MutableStateFlow<BreathingNorm?>(null)
    val breathingNorm: StateFlow<BreathingNorm?> = _breathingNorm

    init {
        viewModelScope.launch {
            _breathingNorm.value = breathingNormRepository.getBreathingNorm()
        }
    }

    fun setBreathingNorm(min: Float, max: Float, formulaType: Int) {
        viewModelScope.launch {
            try {
                setBreathingNormUseCase(min, max, formulaType)
                _breathingNorm.value = breathingNormRepository.getBreathingNorm()
            } catch (e: IllegalArgumentException) {
                // Некорректный ввод — игнорируем (UI должен валидировать)
            }
        }
    }

    fun setBreathingNorm(norm: BreathingNorm) {
        viewModelScope.launch {
            try {
                setBreathingNormUseCase(norm.minNormal, norm.maxNormal, norm.formulaType)
                _breathingNorm.value = norm
            } catch (e: IllegalArgumentException) {
                // Некорректный ввод — игнорируем (UI должен валидировать)
            }
        }
    }

    /** Добавляет замер на конкретную дату/время. */
    fun addMeasurement(value: Float, timestamp: Long, note: String?) {
        viewModelScope.launch {
            try {
                require(value > 0f) { "Значение замера должно быть положительным" }
                dutyMeasurementRepository.insertMeasurement(
                    DutyMeasurement(
                        value = value,
                        timestamp = timestamp,
                        note = note?.takeIf { it.isNotBlank() }
                    )
                )
            } catch (e: IllegalArgumentException) {
                // Валидация на стороне UI
            }
        }
    }

    fun deleteMeasurement(measurement: DutyMeasurement) {
        viewModelScope.launch {
            dutyMeasurementRepository.deleteMeasurement(measurement)
        }
    }
}
