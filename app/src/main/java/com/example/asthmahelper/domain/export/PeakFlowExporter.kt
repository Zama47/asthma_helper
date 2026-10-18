package com.example.asthmahelper.domain.export

import com.example.asthmahelper.domain.clinical.Zone
import com.example.asthmahelper.domain.clinical.percentOfBest
import com.example.asthmahelper.domain.clinical.zoneOf
import com.example.asthmahelper.domain.clinical.zoneOfPercent
import com.example.asthmahelper.domain.model.BreathingNorm
import com.example.asthmahelper.domain.model.DutyMeasurement
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Дневная сводка ПСВ: утро/вечер, среднее и разброс — то, что удобно показать врачу. */
data class DailyPeakFlow(
    val dayMillis: Long,
    val morning: Float?,
    val evening: Float?,
    val average: Float,
    val max: Float,
    val min: Float,
    val count: Int
)

private const val CSV_SEPARATOR = ";"
private const val CRLF = "\r\n"

/** Группирует замеры по дням и считает дневные агрегаты (по возрастанию даты). */
fun dailyAggregates(measurements: List<DutyMeasurement>): List<DailyPeakFlow> = measurements
    .groupBy { startOfDay(it.timestamp) }
    .toSortedMap()
    .map { (day, list) ->
        val sorted = list.sortedBy { it.timestamp }
        DailyPeakFlow(
            dayMillis = day,
            morning = sorted.firstOrNull()?.value,
            evening = if (sorted.size > 1) sorted.last().value else null,
            average = sorted.map { it.value }.average().toFloat(),
            max = sorted.maxOf { it.value },
            min = sorted.minOf { it.value },
            count = sorted.size
        )
    }

/**
 * CSV со всеми замерами: дата, время, ПСВ, процент от личного максимума, зона и заметка.
 * Разделитель «;» и CRLF — так файл корректно открывается в Excel.
 */
fun measurementsToCsv(measurements: List<DutyMeasurement>, norm: BreathingNorm?, best: Float?): String {
    val header = listOf("Дата", "Время", "ПСВ, л/мин", "% от максимума", "Зона", "Заметка")
    val rows = measurements.sortedBy { it.timestamp }.map { measurement ->
        val zone = if (best != null) zoneOfPercent(measurement.value, best) else zoneOf(measurement.value, norm)
        listOf(
            formatDate(measurement.timestamp),
            formatTime(measurement.timestamp),
            measurement.value.toInt().toString(),
            percentOfBest(measurement.value, best)?.let { formatNumber(it) } ?: "",
            zoneTitle(zone),
            measurement.note.orEmpty()
        )
    }
    return buildCsv(header, rows)
}

/** CSV с дневными агрегатами (утро/вечер/среднее/максимум/минимум). */
fun dailyAggregatesToCsv(aggregates: List<DailyPeakFlow>): String {
    val header = listOf("Дата", "Утро", "Вечер", "Среднее", "Максимум", "Минимум", "Замеров")
    val rows = aggregates.map { day ->
        listOf(
            formatDate(day.dayMillis),
            day.morning?.toInt()?.toString() ?: "",
            day.evening?.toInt()?.toString() ?: "",
            formatNumber(day.average),
            day.max.toInt().toString(),
            day.min.toInt().toString(),
            day.count.toString()
        )
    }
    return buildCsv(header, rows)
}

/** Полный отчёт: сначала все замеры, затем дневная сводка. */
fun buildPeakFlowExportCsv(measurements: List<DutyMeasurement>, norm: BreathingNorm?, best: Float?): String =
    buildString {
        append("# Пикфлоуметрия — все замеры").append(CRLF)
        append(measurementsToCsv(measurements, norm, best))
        append(CRLF)
        append("# Дневная сводка").append(CRLF)
        append(dailyAggregatesToCsv(dailyAggregates(measurements)))
    }

private fun buildCsv(header: List<String>, rows: List<List<String>>): String = buildString {
    append(header.joinToString(CSV_SEPARATOR) { escape(it) }).append(CRLF)
    rows.forEach { row ->
        append(row.joinToString(CSV_SEPARATOR) { escape(it) }).append(CRLF)
    }
}

private fun escape(value: String): String = if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
    "\"" + value.replace("\"", "\"\"") + "\""
} else {
    value
}

private fun zoneTitle(zone: Zone): String = when (zone) {
    Zone.GREEN -> "Зелёная"
    Zone.YELLOW -> "Жёлтая"
    Zone.RED -> "Красная"
}

private fun formatNumber(value: Float): String = String.format(Locale.US, "%.0f", value)

private fun formatDate(millis: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date(millis))

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
