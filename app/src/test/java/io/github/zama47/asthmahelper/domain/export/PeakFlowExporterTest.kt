package io.github.zama47.asthmahelper.domain.export

import io.github.zama47.asthmahelper.domain.model.BreathingNorm
import io.github.zama47.asthmahelper.domain.model.DutyMeasurement
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeakFlowExporterTest {

    private val norm = BreathingNorm(minNormal = 300f, maxNormal = 500f, formulaType = 0)

    @Test
    fun dailyAggregates_groupsByDayWithMorningAndEvening() {
        val data = listOf(
            measurement(400f, at(2024, 0, 10, 8)),
            measurement(500f, at(2024, 0, 10, 20)),
            measurement(450f, at(2024, 0, 10, 13)),
            measurement(350f, at(2024, 0, 11, 9))
        )

        val aggregates = dailyAggregates(data)

        assertEquals(2, aggregates.size)
        val first = aggregates.first()
        assertEquals(400f, first.morning!!, 0.001f)
        assertEquals(500f, first.evening!!, 0.001f)
        assertEquals(450f, first.average, 0.001f)
        assertEquals(500f, first.max, 0.001f)
        assertEquals(400f, first.min, 0.001f)
        assertEquals(3, first.count)
        assertEquals(null, aggregates.last().evening)
    }

    @Test
    fun measurementsToCsv_writesHeaderAndRowsWithZoneAndPercent() {
        val data = listOf(measurement(400f, at(2024, 0, 10, 8)))

        val csv = measurementsToCsv(data, norm = norm, best = 500f)
        val lines = csv.trim().split("\r\n")

        assertEquals("Дата;Время;ПСВ, л/мин;% от максимума;Зона;Заметка", lines[0])
        assertEquals("10.01.2024;08:00;400;80;Зелёная;", lines[1])
    }

    @Test
    fun measurementsToCsv_usesNormZonesWhenBestUnknown() {
        val data = listOf(measurement(100f, at(2024, 0, 10, 8)))

        val csv = measurementsToCsv(data, norm = norm, best = null)
        val line = csv.trim().split("\r\n")[1]

        assertTrue(line.contains(";Красная;"))
        assertTrue(line.contains(";;Красная;"))
    }

    @Test
    fun measurementsToCsv_escapesSeparatorsInNote() {
        val data = listOf(measurement(400f, at(2024, 0, 10, 8), note = "до;после"))

        val csv = measurementsToCsv(data, norm = norm, best = 500f)

        assertTrue(csv.contains("\"до;после\""))
    }

    @Test
    fun buildPeakFlowExportCsv_containsBothSections() {
        val data = listOf(measurement(400f, at(2024, 0, 10, 8)))

        val report = buildPeakFlowExportCsv(data, norm, best = 500f)

        assertTrue(report.contains("# Пикфлоуметрия — все замеры"))
        assertTrue(report.contains("# Дневная сводка"))
        assertTrue(report.contains("Дата;Утро;Вечер;Среднее;Максимум;Минимум;Замеров"))
    }

    private fun measurement(value: Float, timestamp: Long, note: String? = null) =
        DutyMeasurement(value = value, timestamp = timestamp, note = note)

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
