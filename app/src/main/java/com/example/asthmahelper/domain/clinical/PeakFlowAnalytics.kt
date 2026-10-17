package com.example.asthmahelper.domain.clinical

import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.model.DutyMeasurement
import java.util.Calendar
import kotlin.math.abs

/** Три зоны нормы по классической пикфлоуметрии. */
enum class Zone {
    GREEN,
    YELLOW,
    RED
}

/**
 * Определяет зону замера по норме.
 * Зелёная — 80–100% от максимума, жёлтая — 50–80%, красная — ниже 50%.
 */
fun zoneOf(value: Float, norm: BreathingNorm?): Zone {
    if (norm == null) return Zone.GREEN
    return when {
        value >= norm.maxNormal * 0.8f -> Zone.GREEN
        value >= norm.redZoneThreshold -> Zone.YELLOW
        else -> Zone.RED
    }
}

/** Результат расчёта суточной вариабельности за одни сутки. */
data class DailyVariability(
    val dayMillis: Long,
    val morning: Float,
    val morningTime: Long,
    val evening: Float,
    val eveningTime: Long,
    val max: Float,
    val percent: Float
)

/**
 * Суточная вариабельность (GINA) по последним суткам, где есть минимум два замера.
 * Вариабельность (%) = |Вечернее − Утреннее| / Максимум за сутки × 100 %.
 */
fun computeDailyVariability(measurements: List<DutyMeasurement>): DailyVariability? = measurements
    .groupBy { startOfDay(it.timestamp) }
    .mapNotNull { (day, list) ->
        if (list.size < 2) return@mapNotNull null
        val sorted = list.sortedBy { it.timestamp }
        val max = sorted.maxOf { it.value }
        if (max <= 0f) return@mapNotNull null
        val first = sorted.first()
        val last = sorted.last()
        DailyVariability(
            dayMillis = day,
            morning = first.value,
            morningTime = first.timestamp,
            evening = last.value,
            eveningTime = last.timestamp,
            max = max,
            percent = abs(last.value - first.value) / max * 100f
        )
    }
    .maxByOrNull { it.dayMillis }

/** Оценка суточной вариабельности: < 15% норма, 15–20% граница, > 20% повышенная. */
enum class VariabilityLevel {
    NORMAL,
    BORDERLINE,
    HIGH
}

fun variabilityLevel(percent: Float): VariabilityLevel = when {
    percent < 15f -> VariabilityLevel.NORMAL
    percent <= 20f -> VariabilityLevel.BORDERLINE
    else -> VariabilityLevel.HIGH
}

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
