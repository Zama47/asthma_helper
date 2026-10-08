package com.example.asthmahelper.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asthmahelper.domain.model.AsthmaAttackLog
import com.example.asthmahelper.domain.model.AttackSeverity
import com.example.asthmahelper.domain.model.MedicationLog
import com.example.asthmahelper.domain.model.MedicationSchedule
import com.example.asthmahelper.domain.repository.AsthmaAttackLogRepository
import com.example.asthmahelper.domain.repository.MedicationLogRepository
import com.example.asthmahelper.domain.repository.MedicationScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Год и месяц (month: 0..11, как в Calendar). */
data class MonthYear(val year: Int, val month: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AsthmaCalendarViewModel @Inject constructor(
    private val attackRepository: AsthmaAttackLogRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val logRepository: MedicationLogRepository
) : ViewModel() {

    val attacks: StateFlow<List<AsthmaAttackLog>> =
        attackRepository.getAllAttacks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _displayedMonth = MutableStateFlow(currentMonth())
    val displayedMonth: StateFlow<MonthYear> = _displayedMonth

    /**
     * Дни отображаемого месяца, в которые было пропущено лекарство:
     * есть активное расписание на этот день и нет отметки «принято».
     */
    val missedDays: StateFlow<Set<Long>> =
        combine(_displayedMonth, scheduleRepository.getActiveSchedules()) { month, schedules ->
            month to schedules
        }.flatMapLatest { (month, schedules) ->
            if (schedules.isEmpty()) {
                flowOf(emptySet())
            } else {
                logRepository.getLogsForPeriod(
                    monthStartMillis(month),
                    monthEndMillis(month)
                ).map { logs -> missedDaysForMonth(month, schedules, logs) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun goToMonth(month: MonthYear) {
        _displayedMonth.update { month }
    }

    /** Добавление нового приступа или обновление существующего (id != null). */
    fun saveAttack(
        id: Long? = null,
        timestamp: Long,
        severity: AttackSeverity,
        rescueDoses: Int,
        triggers: List<String>,
        location: String?,
        notes: String?
    ) {
        val attack = AsthmaAttackLog(
            id = id ?: 0L,
            timestamp = timestamp,
            severity = severity,
            rescueDoses = rescueDoses,
            triggers = triggers,
            location = location,
            notes = notes?.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            if (id == null) {
                attackRepository.insertAttack(attack)
            } else {
                attackRepository.updateAttack(attack)
            }
        }
    }

    /** Однотапная запись приступа по кнопке SOS (защита от двойного нажатия — 3 секунды). */
    fun addSosAttack(): Long {
        val now = System.currentTimeMillis()
        if (now - lastSosTimestamp < 3_000L) return now
        lastSosTimestamp = now
        viewModelScope.launch {
            attackRepository.insertAttack(
                AsthmaAttackLog(
                    timestamp = now,
                    severity = AttackSeverity.MEDIUM,
                    rescueDoses = 0,
                    triggers = emptyList(),
                    location = null,
                    notes = "Отмечено кнопкой «У меня приступ!»"
                )
            )
        }
        return now
    }

    fun deleteAttack(attack: AsthmaAttackLog) {
        viewModelScope.launch { attackRepository.deleteAttack(attack) }
    }

    private var lastSosTimestamp = 0L

    private fun missedDaysForMonth(
        month: MonthYear,
        schedules: List<MedicationSchedule>,
        logs: List<MedicationLog>
    ): Set<Long> {
        val result = mutableSetOf<Long>()
        val now = System.currentTimeMillis()
        val today = startOfDayMillis(now)

        val cal = Calendar.getInstance().apply {
            timeInMillis = monthStartMillis(month)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = monthStartMillis(nextMonth(month))

        while (cal.timeInMillis < end) {
            val day = cal.timeInMillis
            if (day <= today) {
                val isoDay = isoDayOfWeek(cal)
                val daySchedules = schedules.filter { it.daysOfWeek.contains(isoDay.toString()) }
                val missed = daySchedules.any { schedule ->
                    day + schedule.timeOfDay < now &&
                        logs.none { it.scheduleId == schedule.id && it.date == day && it.taken }
                }
                if (daySchedules.isNotEmpty() && missed) result += day
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }
}

// ---------- Чистые функции расчётов ----------

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

fun startOfDayMillis(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * Дней без приступов подряд, считая назад от сегодняшнего дня.
 * 0 — приступ был сегодня, -1 — приступов не записано вовсе.
 */
fun calculateStreak(attacks: List<AsthmaAttackLog>): Int {
    if (attacks.isEmpty()) return -1
    val lastAttackDay = startOfDayMillis(attacks.maxOf { it.timestamp })
    val today = startOfDayMillis(System.currentTimeMillis())
    if (lastAttackDay >= today) return 0
    return ((today - lastAttackDay) / MILLIS_PER_DAY).toInt()
}

/** Ночной приступ (00:00–05:59) — критический показатель по GINA. */
fun isNightAttack(timestamp: Long): Boolean {
    val hour = Calendar.getInstance().apply { timeInMillis = timestamp }
        .get(Calendar.HOUR_OF_DAY)
    return hour in 0..5
}

fun currentMonth(): MonthYear =
    with(Calendar.getInstance()) {
        MonthYear(get(Calendar.YEAR), get(Calendar.MONTH))
    }

/** Месяц, в который попадает указанный момент времени. */
fun monthOf(timestamp: Long): MonthYear =
    with(Calendar.getInstance()) {
        timeInMillis = timestamp
        MonthYear(get(Calendar.YEAR), get(Calendar.MONTH))
    }

fun nextMonth(month: MonthYear): MonthYear =
    if (month.month == 11) MonthYear(month.year + 1, 0) else MonthYear(month.year, month.month + 1)

fun prevMonth(month: MonthYear): MonthYear =
    if (month.month == 0) MonthYear(month.year - 1, 11) else MonthYear(month.year, month.month - 1)

fun monthStartMillis(month: MonthYear): Long =
    Calendar.getInstance().apply {
        timeInMillis = 0
        set(Calendar.YEAR, month.year)
        set(Calendar.MONTH, month.month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** Последняя миллисекунда месяца (для диапазонов с BETWEEN). */
fun monthEndMillis(month: MonthYear): Long = monthStartMillis(nextMonth(month)) - 1

/** 1 = Пн … 7 = Вс — как в строке daysOfWeek расписаний. */
fun isoDayOfWeek(cal: Calendar): Int = when (cal.get(Calendar.DAY_OF_WEEK)) {
    Calendar.SUNDAY -> 7
    else -> cal.get(Calendar.DAY_OF_WEEK) - 1
}

/** Множественная форма: день/дня/дней. */
fun daysPlural(value: Int): String {
    val n10 = value % 10
    val n100 = value % 100
    return when {
        n10 == 1 && n100 != 11 -> "день"
        n10 in 2..4 && n100 !in 12..14 -> "дня"
        else -> "дней"
    }
}

/** Множественная форма: приступ/приступа/приступов. */
fun attacksPlural(value: Int): String {
    val n10 = value % 10
    val n100 = value % 100
    return when {
        n10 == 1 && n100 != 11 -> "приступ"
        n10 in 2..4 && n100 !in 12..14 -> "приступа"
        else -> "приступов"
    }
}
