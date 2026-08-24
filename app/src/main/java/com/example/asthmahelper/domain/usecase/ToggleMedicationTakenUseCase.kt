package com.example.asthmahelper.domain.usecase

import com.example.asthmahelper.domain.model.MedicationLog
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Отмечает/снимает отметку приёма лекарства для расписания в конкретный день.
 * Ленивая инициализация: если записи за день ещё нет — создаёт её.
 */
class ToggleMedicationTakenUseCase @Inject constructor(
    private val logRepository: MedicationLogRepository
) {
    suspend operator fun invoke(scheduleId: Long, date: Long, taken: Boolean) {
        val dayStart = startOfDay(date)
        val existing = logRepository.getLogForScheduleAndDate(scheduleId, dayStart)
        if (existing != null) {
            logRepository.updateLog(existing.copy(taken = taken))
        } else {
            logRepository.insertLog(
                MedicationLog(
                    scheduleId = scheduleId,
                    date = dayStart,
                    taken = taken
                )
            )
        }
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
