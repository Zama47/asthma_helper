package com.example.asthmahelper.domain.clinical

import com.example.asthmahelper.domain.model.AsthmaAttackLog
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

/** Окно личного максимума по умолчанию — 14 дней (рекомендация GINA). */
const val PERSONAL_BEST_WINDOW_DAYS = 14

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

/**
 * Личный максимум ПСВ: максимальный замер за последние [windowDays] дней.
 * Возвращает null, если в окне нет замеров.
 */
fun personalBest(
    measurements: List<DutyMeasurement>,
    windowDays: Int = PERSONAL_BEST_WINDOW_DAYS,
    now: Long = System.currentTimeMillis()
): Float? {
    val from = now - windowDays * MILLIS_PER_DAY
    return measurements
        .filter { it.timestamp in from..now }
        .maxOfOrNull { it.value }
}

/** Процент замера от личного максимума. null, если максимум неизвестен или неположителен. */
fun percentOfBest(value: Float, best: Float?): Float? {
    if (best == null || best <= 0f) return null
    return value / best * 100f
}

/**
 * Зона по проценту от личного максимума (GINA): зелёная — от 80%, жёлтая — 50–80%, красная — ниже 50%.
 * Если личный максимум неизвестен, считаем зону зелёной.
 */
fun zoneOfPercent(value: Float, best: Float?): Zone {
    if (best == null || best <= 0f) return Zone.GREEN
    return when {
        value >= best * 0.8f -> Zone.GREEN
        value >= best * 0.5f -> Zone.YELLOW
        else -> Zone.RED
    }
}

/** Признак начинающегося обострения: устойчивое падение ПСВ ниже обычного уровня. */
data class ExacerbationWarning(val consecutiveDays: Int, val worstPercentOfBest: Float, val sinceMillis: Long)

/**
 * Ищет обострение по GINA: два и более дней подряд максимальный ПСВ дня ниже
 * [dropPercent]% от личного максимума. Возвращает описание, если такое падение есть.
 *
 * Дни без замеров разрывают серию, поэтому «тихий» пропуск не считается ухудшением.
 */
fun detectExacerbation(
    measurements: List<DutyMeasurement>,
    best: Float?,
    now: Long = System.currentTimeMillis(),
    dropPercent: Float = 20f,
    minConsecutiveDays: Int = 2,
    windowDays: Int = PERSONAL_BEST_WINDOW_DAYS
): ExacerbationWarning? {
    if (best == null || best <= 0f) return null
    val threshold = best * (1f - dropPercent / 100f)
    val from = now - windowDays * MILLIS_PER_DAY
    val byDay = measurements
        .filter { it.timestamp in from..now }
        .groupBy { startOfDay(it.timestamp) }
    if (byDay.isEmpty()) return null

    // Серия должна доходить до сегодня или вчера, иначе это архив, а не текущее состояние.
    val latestDay = byDay.keys.max()
    if (startOfDay(now) - latestDay > MILLIS_PER_DAY) return null

    var cursor = latestDay
    var streak = 0
    var worstPercent = 100f
    var since = latestDay
    while (true) {
        val dayValues = byDay[cursor] ?: break
        val dailyMax = dayValues.maxOf { it.value }
        if (dailyMax >= threshold) break
        streak++
        val percent = dailyMax / best * 100f
        if (percent < worstPercent) worstPercent = percent
        since = cursor
        cursor -= MILLIS_PER_DAY
    }

    return if (streak >= minConsecutiveDays) {
        ExacerbationWarning(consecutiveDays = streak, worstPercentOfBest = worstPercent, sinceMillis = since)
    } else {
        null
    }
}

/** Сколько доз спасательного ингалятора отмечено сегодня и за последние 7 дней. */
data class RescueDoseStats(val today: Int, val thisWeek: Int)

/** Порог частого использования спасательного ингалятора за неделю (GINA). */
const val HIGH_RESCUE_DOSES_PER_WEEK = 8

fun rescueDoseStats(attacks: List<AsthmaAttackLog>, now: Long = System.currentTimeMillis()): RescueDoseStats {
    val todayStart = startOfDay(now)
    val weekStart = todayStart - 6 * MILLIS_PER_DAY
    val today = attacks.filter { it.timestamp >= todayStart }.sumOf { it.rescueDoses }
    val week = attacks.filter { it.timestamp >= weekStart }.sumOf { it.rescueDoses }
    return RescueDoseStats(today = today, thisWeek = week)
}

/** Частое использование спасательного ингалятора — повод пересмотреть терапию. */
fun isRescueUseHigh(weekDoses: Int): Boolean = weekDoses >= HIGH_RESCUE_DOSES_PER_WEEK

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
