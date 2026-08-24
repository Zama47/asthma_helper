package com.example.asthmahelper.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.model.Medicine
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import com.example.asthmahelper.domain.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationsViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val scheduleRepository: MedicationScheduleRepository
) : ViewModel() {

    val medicines: StateFlow<List<Medicine>> =
        medicineRepository.getAllMedicines()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val schedules: StateFlow<List<MedicationSchedule>> =
        scheduleRepository.getActiveSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _popularMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    val popularMedicines: StateFlow<List<Medicine>> = _popularMedicines

    fun loadMedicines() {
        viewModelScope.launch {
            medicineRepository.getPopularMedicines().collect { list ->
                _popularMedicines.value = list
            }
        }
    }

    fun addCustomMedicine(name: String, description: String) {
        viewModelScope.launch {
            medicineRepository.insertMedicine(
                Medicine(
                    name = name,
                    description = description,
                    isPopular = false
                )
            )
        }
    }

    /** Создаёт пользовательское лекарство и сразу расписание приёма. */
    fun addCustomMedicineWithSchedule(
        name: String,
        description: String,
        dose: String,
        timeOfDayMinutes: Long,
        daysOfWeek: String
    ) {
        viewModelScope.launch {
            val medicineId = medicineRepository.insertMedicine(
                Medicine(
                    name = name,
                    description = description,
                    isPopular = false
                )
            )
            scheduleRepository.insertSchedule(
                MedicationSchedule(
                    medicineId = medicineId,
                    dose = dose,
                    timeOfDay = timeOfDayMinutes * 60_000,
                    daysOfWeek = daysOfWeek,
                    isActive = true
                )
            )
        }
    }

    /** Добавляет расписание для существующего лекарства. */
    fun addSchedule(medicineId: Long, dose: String, timeOfDayMinutes: Long, daysOfWeek: String) {
        viewModelScope.launch {
            scheduleRepository.insertSchedule(
                MedicationSchedule(
                    medicineId = medicineId,
                    dose = dose,
                    timeOfDay = timeOfDayMinutes * 60_000,
                    daysOfWeek = daysOfWeek,
                    isActive = true
                )
            )
        }
    }

    fun deleteSchedule(schedule: MedicationSchedule) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule)
        }
    }
}
