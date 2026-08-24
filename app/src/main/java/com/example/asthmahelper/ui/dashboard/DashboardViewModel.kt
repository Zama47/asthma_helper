package com.example.asthmahelper.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.model.DutyMeasurement
import com.example.asthmahelper.domain.model.MedicationLog
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import com.example.asthmahelper.domain.usecase.AddMeasurementUseCase
import com.example.asthmahelper.domain.usecase.ToggleMedicationTakenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TodayMedicationUiItem(
    val schedule: MedicationSchedule,
    val taken: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dutyMeasurementRepository: DutyMeasurementRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val logRepository: MedicationLogRepository,
    breathingNormRepository: BreathingNormRepository,
    private val addMeasurementUseCase: AddMeasurementUseCase,
    private val toggleTakenUseCase: ToggleMedicationTakenUseCase
) : ViewModel() {

    /** Последние замеры (для сводки «Последний замер»). */
    val recentMeasurements: StateFlow<List<DutyMeasurement>> =
        dutyMeasurementRepository.getAllMeasurements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Норма дыхания (для сводки «Норма: X–X»). */
    val breathingNorm = kotlinx.coroutines.flow.flow {
        emit(breathingNormRepository.getBreathingNorm())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as BreathingNorm?)

    /** Приёмы на сегодня с отметками. */
    val todayMedications: StateFlow<List<TodayMedicationUiItem>> =
        scheduleRepository.getActiveSchedules()
            .flatMapLatest { schedules ->
                if (schedules.isEmpty()) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    combine(
                        schedules.map { schedule -> logsForToday(schedule.id) }
                    ) { logsPerSchedule: Array<List<MedicationLog>> ->
                        schedules.mapIndexed { index, schedule ->
                            TodayMedicationUiItem(
                                schedule = schedule,
                                taken = logsPerSchedule.getOrNull(index)
                                    ?.firstOrNull()?.taken ?: false
                            )
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun logsForToday(scheduleId: Long) =
        logRepository.getLogsForDate(todayStart)

    fun addMeasurement(value: Float, note: String?) {
        viewModelScope.launch {
            runCatching { addMeasurementUseCase(value, note) }
            // Ошибки валидации игнорируем — UI проверяет ввод заранее
        }
    }

    fun toggleMedication(item: TodayMedicationUiItem, taken: Boolean) {
        viewModelScope.launch {
            toggleTakenUseCase(item.schedule.id, System.currentTimeMillis(), taken)
        }
    }

    companion object {
        val todayStart: Long by lazy {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
