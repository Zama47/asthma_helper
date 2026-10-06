package com.example.asthmahelper.ui.dashboard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.data.api.WeatherApiService
import com.example.asthmahelper.domain.model.AirQualityInfo
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.model.DutyMeasurement
import com.example.asthmahelper.domain.model.MedicationLog
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.model.PollenInfo
import com.example.asthmahelper.domain.model.PollenLevel
import com.example.asthmahelper.domain.model.Plant
import com.example.asthmahelper.domain.repository.BreathingNormRepository
import com.example.asthmahelper.domain.repository.DutyMeasurementRepository
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import com.example.asthmahelper.domain.usecase.AddMeasurementUseCase
import com.example.asthmahelper.domain.usecase.ToggleMedicationTakenUseCase
import com.example.asthmahelper.util.WeatherSettingsKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    dutyMeasurementRepository: DutyMeasurementRepository,
    scheduleRepository: MedicationScheduleRepository,
    private val logRepository: MedicationLogRepository,
    breathingNormRepository: BreathingNormRepository,
    private val addMeasurementUseCase: AddMeasurementUseCase,
    private val toggleTakenUseCase: ToggleMedicationTakenUseCase,
    private val weatherApi: WeatherApiService,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    // Координаты по умолчанию (Москва), если город не выбран вручную
    private val defaultLat = 55.7558
    private val defaultLon = 37.6173

    data class WeatherDashboardData(
        val airQuality: AirQualityInfo?,
        val pollen: PollenInfo?
    )

    /** Данные о погоде, качестве воздуха и пыльце из Open-Meteo API. */
    val weatherData: StateFlow<WeatherDashboardData> = flow {
        // Город, выбранный на экране погоды, либо Москва по умолчанию
        val prefs = dataStore.data.first()
        val lat = prefs[WeatherSettingsKeys.CITY_LAT] ?: defaultLat
        val lon = prefs[WeatherSettingsKeys.CITY_LON] ?: defaultLon

        try {
            val air = weatherApi.getAirQuality(lat, lon)
            val airCurrent = air.current

            val airQuality = AirQualityInfo(
                aqi = airCurrent.europeanAqi?.toInt() ?: 0,
                pm25 = airCurrent.pm25 ?: 0.0,
                pm10 = airCurrent.pm10 ?: 0.0,
                o3 = airCurrent.ozone ?: 0.0,
                no2 = airCurrent.nitrogenDioxide ?: 0.0
            )

            // Пыльца: берём максимум из активных растений
            val pollen = buildPollen(airCurrent)

            emit(WeatherDashboardData(airQuality = airQuality, pollen = pollen))
        } catch (_: Exception) {
            // При ошибке API — эмитим пустые данные, виджеты покажут «Нет данных»
            emit(WeatherDashboardData(airQuality = null, pollen = null))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WeatherDashboardData(airQuality = null, pollen = null)
    )

    /** Последние замеры (для сводки «Последний замер»). */
    val recentMeasurements: StateFlow<List<DutyMeasurement>> =
        dutyMeasurementRepository.getAllMeasurements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Норма дыхания (для сводки «Норма: X–X»). */
    val breathingNorm: StateFlow<BreathingNorm?> = flow {
        emit(breathingNormRepository.getBreathingNorm())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Приёмы на сегодня с отметками (лог ищется по конкретному расписанию). */
    val todayMedications: StateFlow<List<TodayMedicationUiItem>> =
        scheduleRepository.getActiveSchedules()
            .flatMapLatest { schedules ->
                if (schedules.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // Реактивные Flow: при отметке приёма Room эмитит заново
                    val flows: List<Flow<MedicationLog?>> = schedules.map { schedule ->
                        logRepository.getLogForScheduleAndDateFlow(schedule.id, todayStart)
                    }
                    combine(flows) { logs ->
                        schedules.mapIndexed { index, schedule ->
                            TodayMedicationUiItem(
                                schedule = schedule,
                                taken = logs.getOrNull(index)?.taken == true
                            )
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Собирает пыльцу из ответа API: активные растения с концентрацией ≥ 1 зерно/м³. */
    private fun buildPollen(airCurrent: com.example.asthmahelper.data.api.dto.CurrentAirQualityDto): PollenInfo {
        data class Entry(val name: String, val value: Double?)
        val entries = listOf(
            Entry("Берёза", airCurrent.birchPollen),
            Entry("Ольха", airCurrent.alderPollen),
            Entry("Злаковые", airCurrent.grassPollen),
            Entry("Полынь", airCurrent.mugwortPollen),
            Entry("Амброзия", airCurrent.ragweedPollen)
        )
        val plants = entries
            .filter { (it.value ?: 0.0) >= 1.0 }
            .map { Plant(it.name, PollenLevel.fromConcentration(it.value ?: 0.0)) }
            .sortedByDescending { it.pollenLevel.ordinal }
        val overall = plants.maxOfOrNull { it.pollenLevel } ?: PollenLevel.NONE
        return PollenInfo(level = overall, activePlants = plants)
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
